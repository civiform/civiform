const fs = require('fs');
const prefixes = {
  'even': 'EVEN',
  'focus': 'FOCUS',
  'focus-within': 'FOCUS_WITHIN',
  'hover': 'HOVER',
  'disabled': 'DISABLED',
  'sm': 'RESPONSIVE_SM',
  'md': 'RESPONSIVE_MD',
  'lg': 'RESPONSIVE_LG',
  'xl': 'RESPONSIVE_XL',
  '2xl': 'RESPONSIVE_2XL'
};

const htmlTags = fs.readFileSync('./tags.txt', 'utf8').split('\n');

class StylesJavaReader {
  constructor(file_contents) {
    this.file_contents = file_contents;
    this.rgx_key = /(?<= +public +static +final +String +)([0-9A-Z_]+)/g;
    this.rgx_val = /(?<= +public +static +final +String +[0-9A-Z_]+ += +")([a-z0-9-/]+)/g;
  }

  getMatches(matches) {
    let count = 1
    for (const line of this.file_contents) {
      let match_key = line.match(this.rgx_key);
      let match_val = line.match(this.rgx_val);

      if (Array.isArray(match_key) && Array.isArray(match_val)) {
        if (match_key.length === 1 && match_val.length === 1) {
          matches[match_key[0]] = match_val[0];
          if (false & count % 200 === 0) {
            console.log(match_key[0]);
          }
        } else {
          console.log("strange line in 'Styles.java' at line " + count.toString());
        }
      }

      count += 1
    }
  }
}

function getStyles() {
  const matches = {};
  let folder = './app/views/style/'
  try {
    let specialFiles = ['Styles.java', 'BaseStyles.java'];
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

var PROCESSED_TS = false;
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

        var output = []
        let matchIter = content.match(/(?<=Styles\.)([0-9A-Z_]+)/g);

        if (matchIter) {
          for (const m of matchIter) {
            let s = styleDict[m];
            
            if (s === undefined) {
              console.log(m);
            } else {
              output.push(s)
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
                output.push(prefix + ':' + s)
              }
            }
          }
        }

        const assetsFolder = './app/assets/javascripts/';
        let files = fs.readdirSync(assetsFolder);

        if (PROCESSED_TS === false) {
          for (const f of files) {
            let data = fs.readFileSync(assetsFolder + f, 'utf8');
            let matches = data.matchAll(/"([\w-/:]+)"/g);
            for (m of matches) {
              output.push(m);
            }
          }

          output.push("button");

          // Had to manually push all the html tags for some reason
          for (const t of htmlTags) {
            output.push(t);
          }

          PROCESSED_TS = true;
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

