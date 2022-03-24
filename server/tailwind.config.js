const fs = require('fs')

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

// Files to parse for style dictonary using regex
const styleFolder = './app/views/style/'
const styleFiles = ['Styles.java', 'ReferenceClasses.java']

// Prefixes for media queries
const PREFIXES = [
  'even',
  'focus',
  'focus-within',
  'hover',
  'disabled',
  'sm',
  'md',
  'lg',
  'xl',
  '2xl',
]

// Used to read Styles.java and ReferenceClasses.java to get dictionary mapping
// for all possible base styles (no prefixes)
function addStyleDictMatches(matches, file_contents) {
  let lineNmbr = 0
  for (const line of file_contents) {
    lineNmbr++

    let match_key = line.match(JAVA_STYLE_KEY_REGEX)
    let match_val = line.match(JAVA_STYLE_VALUE_REGEX)

    // Both 'variable' and tailwind str are probably on same line
    // Even though java probably doesn't require it
    if (Array.isArray(match_key) && Array.isArray(match_val)) {
      if (match_key.length === 1 && match_val.length === 1) {
        matches[match_key[0]] = match_val[0]
      } else {
        throw "strange line in 'Styles.java' at line " + lineNmbr.toString()
      }
    }
  }
}

function getStylesDict() {
  const matches = {}
  for (const file of styleFiles) {
    const data = fs.readFileSync(styleFolder + file, 'utf8').split('\n')
    addStyleDictMatches(matches, data)
  }

  return matches
}

const styleDict = getStylesDict()

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
    extract: {
      // Routine to process contents with .java extention. Tailwind has a builtin routine that
      // processes .ts extention files in the `content` list so we dont need to add a method for
      // that
      java: (content) => {
        const output = []

        let matchIter = content.match(STYLE_USAGE_REGEX)

        if (matchIter) {
          for (const tailwindClassId of matchIter) {
            let tailwindClass = styleDict[tailwindClassId]

            if (tailwindClass !== undefined) {
              output.push(tailwindClass)
              // We don't know which, if any, of these prefixes are in use for any class in particular.
              // We therefore use every combination of them.
              for (const prefix of PREFIXES) {
                output.push(prefix + ':' + tailwindClass)
              }
            }
          }
        }

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
