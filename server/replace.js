const fs = require('fs')
const path = require('path')
const assert = require('assert')

/* IMPORTANT
 *
 * If you modify this file, make sure to modify any comments in a
 *    `app/views/style/Styles.java`
 *    `app/views/style/ReferenceClasses.java`
 *    `app/views/style/README.md
 */

// For stylesDict from Styles.java and ReferenceClasses.java
const JAVA_STYLE_KEY_REGEX =
  /(?<= +public +static +final +String +)([0-9A-Z_]+)/g
const JAVA_STYLE_VALUE_REGEX =
  /(?<= +public +static +final +String +[0-9A-Z_]+ += +")([a-z0-9-/.]+)/g

const STYLE_USAGE_REGEX = /(?<=(Styles)\.)([0-9A-Z_]+)/g
const STYLE_USAGE_REGEX_TWO = /(?<= Styles\.)([0-9A-Z_]+)/g

const MEDIA_QUERY_CALL_STR_START = '/(?<=StyleUtils.('

const MEDIA_QUERY_BEGIN = '(?<=StyleUtils.'
const MEDIA_QUERY_CALL_END = '\\()([a-zA-Z0-9_.,\\s]+\\))'

// Files to parse for style dictonary using regex
const STYLE_FOLDER = './app/views/style/'
const STYLE_FILES = ['Styles.java', 'ReferenceClasses.java']

// Prefixes for media queries
// Each list tuple represents ['j2hml method call', 'tailwind css media query']
const PREFIXES = [
  ['even', 'even'],
  ['focus', 'focus'],
  ['focusWithin', 'focus-within'],
  ['hover', 'hover'],
  ['disabled', 'disabled'],
  ['responsiveSmall', 'sm'],
  ['responsiveMedium', 'md'],
  ['responsiveLarge', 'lg'],
  ['responsiveXLarge', 'xl'],
  ['responsive2XLarge', '2xl'],
]

// Used to read Styles.java and ReferenceClasses.java to get dictionary mapping
// for all possible base styles (no prefixes)
function addStyleDictMatches(matches, file_contents) {
  let lineNumber = 0
  for (const line of file_contents) {
    lineNumber++

    let matchKey = line.match(JAVA_STYLE_KEY_REGEX)
    let matchVal = line.match(JAVA_STYLE_VALUE_REGEX)

    // Both 'variable' and tailwind str are probably on same line
    // Even though java probably doesn't require it
    if (Array.isArray(matchKey) && Array.isArray(matchVal)) {
      if (matchKey.length === 1 && matchVal.length === 1) {
        matches[matchKey[0]] = matchVal[0]
      } else {
        throw "strange line in 'Styles.java' at line " + lineNumber.toString()
      }
    }
  }
}

function getStylesDict() {
  const matches = {}
  for (const file of STYLE_FILES) {
    let contents = fs.readFileSync(STYLE_FOLDER + file, 'utf8')
    const data = contents.split('\n')
    addStyleDictMatches(matches, data)
  }

  return matches
}

const styleDict = getStylesDict()

function getStyleUsages(content, prefix = '') {
  const matchIter = content.match(STYLE_USAGE_REGEX)

  const output = []

  if (matchIter) {
    for (const tailwindClassId of matchIter) {
      let tailwindClass = styleDict[tailwindClassId]

      if (tailwindClass !== undefined) {
        output.push(tailwindClassId)
        //if (prefix !== '') {
        //  tailwindClass = prefix + ':' + tailwindClass
        //}
        //output.push(tailwindClass)
      }
    }
  }

  return output
}

function getMediaQueryUsage(content) {
  const output = []

  for (const [methodName, mediaQueryPrefix] of PREFIXES) {
    const MEDIA_QUERY_CALL = new RegExp(
      MEDIA_QUERY_BEGIN + methodName + MEDIA_QUERY_CALL_END,
      'g'
    )

    const mediaQueryMatchIter = content.match(MEDIA_QUERY_CALL)
    if (mediaQueryMatchIter) {
      for (const mediaQueriedContent of mediaQueryMatchIter) {
        output.push(getStyleUsage(mediaQueriedContent, mediaQueryPrefix))
      }
    }
  }

  return output
}

function getAllFiles(dirPath, arrayOfFiles) {
  let files = fs.readdirSync(dirPath)

  arrayOfFiles = arrayOfFiles || []

  files.forEach(function(file) {
    if (fs.statSync(dirPath + "/" + file).isDirectory()) {
      arrayOfFiles = getAllFiles(dirPath + "/" + file, arrayOfFiles)
    } else if (file.endsWith('.java') && file !== "Styles.java" ) {
      arrayOfFiles.push(path.join(__dirname, dirPath, "/", file))
    }
  })

  return arrayOfFiles
}

const javaFilesApp = getAllFiles('./app/views/', [])
const javaFilesTest = getAllFiles('./test/views/', [])

for (const javaFiles of [javaFilesApp, javaFilesTest]) {
  for (const file of javaFiles) {
    let contents = fs.readFileSync(file, 'utf8')
    let styleUsages = getStyleUsages(contents)

    for (const usage of styleUsages) {
      const literal = '"' + styleDict[usage] + '"'

      // Sorry, too lazy to look up regex right now LOL !!!
      for (const beginChar of ['(', ',', ' ']) {
        for (const endChar of [')', ' ', ',', ';']) {
          contents = contents.replaceAll(beginChar+'Styles.'+usage+endChar, beginChar+literal+endChar)
        }
      }
    }

    fs.writeFileSync(file, contents)
  }
}
