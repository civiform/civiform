const fs = require('fs');
const PREFIXES = {
  'even':'even',
  'focus':'focus',
  'focusWithin':'focus-within',
  'hover':'hover',
  'disabled':'disabled',
  'resonsiveSmall':'sm',
  'responsiveMedium':'md',
  'responsiveLarge':'lg',
  'responsiveXLarge':'xl',
  'responsive2XLarge':'2xl'
};

// Used in the main tailwind's method of the transform obj to ensure we only
// read certain files (e.g. typescript) only once, since those have their contents read directly
let processedTs = false;

// Used to get a dictionary mapping for all possible base styles (no prefixes) in
// java files refered to in `function getStyles()`
class StylesJavaReader {

  constructor(file_contents) {
    this.file_contents = file_contents;
  }

  // Styles.java variable as a key, e.g. Styles.BG_BLUE_200
  static get rgxKey() {
    return /(?<= +public +static +final +String +)([0-9A-Z_]+)/g;
  }

  // Tailwind string value refered to by variable, e.g. 'bg-blue-200'
  static get rgxVal() {
    return /(?<= +public +static +final +String +[0-9A-Z_]+ += +")([a-z0-9-/]+)/g;
  }

  getMatches(matches) {
    let count = 1
    for (const line of this.file_contents) {
      let match_key = line.match(this.constructor.rgxKey);
      let match_val = line.match(this.constructor.rgxVal);

      // Both 'variable' and tailwind str are probably on same line
      // Even though java probably doesn't require it
      if (Array.isArray(match_key) && Array.isArray(match_val)) {
        if (match_key.length === 1 && match_val.length === 1) {
          matches[match_key[0]] = match_val[0];
        } else {
          throw "strange line in 'Styles.java' at line " + count.toString();
        }
      }

      count++;
    }
  }
}

class StylesPrefixReader {
  constructor(file_contents, stylesDict) {
    this.file_contents = file_contents;
    this.stylesDict = stylesDict;
  }
}

function getStyles() {
  const matches = {};
  let folder = './app/views/style/'
  try {
    let specialFiles = ['Styles.java', 'BaseStyles.java', 'ReferenceClasses.java'];
    for (file of specialFiles) {
      let data = fs.readFileSync(folder+file, 'utf8').split('\n');
      let stylesReader = new StylesJavaReader(data);
      stylesReader.getMatches(matches);
    }

  }
  catch (error) {
    throw 'error reading Styles.java for tailwindcss processing: ' + error.message;
  }

  return matches;
}

const styleDict = getStyles();

function original(content, output) {
  for (const match of content.matchAll(/"([\w-/:]+)"/g)) {
    output.push(match[1])
    // We don't know which, if any, of these prefixes are in use for any class in particular.
    // We therefore have to use every combination of them.
    for (const prefix of [
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
    ]) {
      output.push(prefix + ':' + match[1])
    }
  }
}

module.exports = {
  purge: {
    enabled: true,
    content: [
      './app/views/**/*.java',
    ],
    extract: {
      java: (content) => {
        return content
      },
    },
    transform: {
      java: (content) => {
        var output = [];

        let matchIter = content.match(/(?<=Styles\.)([0-9A-Z_]+)/g);

        if (matchIter) {
          for (const tailwindClassId of matchIter) {
            let tailwindClass = styleDict[tailwindClassId];
            
            if (tailwindClass !== undefined) {
              output.push(tailwindClass)
              // We don't know which, if any, of these prefixes are in use for any class in particular.
              // We therefore have to use every combination of them.
              for (const prefix of [
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
                      ]) {
                output.push(prefix + ':' + tailwindClass)
              }
            }
          }
        }

        const assetsFolder = './app/assets/javascripts/';
        let files = fs.readdirSync(assetsFolder);

        if (processedTs === false) {
          for (const f of files) {
            let data = fs.readFileSync(assetsFolder + f, 'utf8').split('\n');
            for (const line of data) {
              matches = line.matchAll(/["'][\.a-z0-9/:-]+["']/g);
              for (m of matches) {
                let mr = m[0].replace(/['"]+/g, '');
                output.push(mr);
              }
            }
          }

          // Had to manually push all the html tags for some reason
          const htmlTags = fs.readFileSync('./tags.txt', 'utf8').split('\n');
          for (const t of htmlTags) {
            output.push(t);
          }

          processedTs = true;
        }

        return output
      },
    },
  },

  darkMode: false, // or 'media' or 'class'
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
  variants: {
    extend: {
      backgroundColor: ['disabled', 'odd'],
      textColor: ['disabled'],
      opacity: ['disabled'],
    },
  },
  plugins: [require('@tailwindcss/line-clamp')],
}

