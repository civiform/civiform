const fs = require('fs')
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

const STYLE_USAGE_REGEX = /(?<=(Styles|ReferenceClasses)\.)([0-9A-Z_]+)/g

const MEDIA_QUERY_CALL_STR_START = '/(?<=StyleUtils.('

const MEDIA_QUERY_BEGIN = '(?<=StyleUtils.'
const MEDIA_QUERY_CALL_END = '\\()([a-zA-Z0-9_.,\\s]+\\))'

// Files to parse for style dictonary using regex
const styleFolder = './app/views/style/'
const styleFiles = ['Styles.java', 'ReferenceClasses.java']

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
  for (const file of styleFiles) {
    let contents = fs.readFileSync(styleFolder + file, 'utf8')
    const data = contents.split('\n')
    addStyleDictMatches(matches, data)
  }

  return matches
}

const styleDict = getStylesDict()

function getStyleUsage(content, output, prefix = '') {
  const matchIter = content.match(STYLE_USAGE_REGEX)

  if (matchIter) {
    for (const tailwindClassId of matchIter) {
      let tailwindClass = styleDict[tailwindClassId]

      if (tailwindClass !== undefined) {
        if (prefix !== '') {
          tailwindClass = prefix + ':' + tailwindClass
        }
        output += "['" + tailwindClass + "']"
      }
    }
  }

  return output
}

function getMediaQueryUsage(content, output) {
  for (const [methodName, mediaQueryPrefix] of PREFIXES) {
    const MEDIA_QUERY_CALL = new RegExp(
      MEDIA_QUERY_BEGIN + methodName + MEDIA_QUERY_CALL_END,
      'g'
    )

    const mediaQueryMatchIter = content.match(MEDIA_QUERY_CALL)
    if (mediaQueryMatchIter) {
      for (const mediaQueriedContent of mediaQueryMatchIter) {
        output = getStyleUsage(mediaQueriedContent, output, mediaQueryPrefix)
      }
    }
  }

  return output
}

module.exports = {
  // See:
  //     https://tailwindcss.com/docs/content-configuration
  //
  // And sections:
  //     https://tailwindcss.com/docs/content-configuration#transforming-source-files
  //     https://tailwindcss.com/docs/content-configuration#customizing-extraction-logic
  content: {
    enabled: true,
    // Files we process to identify which styles are being used
    content: ['./app/views/**/*.java', './app/assets/javascripts/*.ts'],
    transform: {
      // Routine to process contents with .java extention. Tailwind has a builtin routine that
      // processes .ts extention files in the `content` list so we dont need to add a method
      // for that
      java: (content) => {
        // It was easier just doing this than incorporating multiline handling into regex
        const contentOneLine = content.replace(/(\r\n|\n|\r|\n\r)/gm, '')
        let output = ''

        output = getStyleUsage(contentOneLine, output)
        output = getMediaQueryUsage(contentOneLine, output)

        return output
      },
    },
  },

  theme: {
    extend: {
      colors: {
        'civiform-white': {
          DEFAULT: '#f8f9fa',
        },
        'seattle-blue': {
          DEFAULT: '#113f9f',
        },
      },
    },
  },
  plugins: [require('@tailwindcss/line-clamp')],
}
