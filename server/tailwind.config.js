const fs = require('fs');

class StylesJavaReader {
  constructor(file_contents) {
    this.file_contents = file_contents;
    this.regexp = /public +static +final +String +([0-9A-Z_]+) += +"(.*)"/g;
  }

  getMatches(matches) {
    for (const match in this.file_contents.match(this.regexp)) {
      matches[match[0]] = match[1];
    }
  }
}

class StylesTsReader {
  constructor(file_contents) {
    this.file_contents = file_contents;
    this.dir = './app/assets/javascripts/';
    //this.regexp = /classList\.{add|remove|toggle}\('(.*)'\)/g;
    this.regexp_a = /"([\w-/:]+)"/g;
    this.regexp_b = /'([\w-/:]+)'/g;
  }

  getMatches(matches) {
    for (const match in this.file_contents.matchAll(this.regexp_a)) {
      matches[match[0]] = match[1];
    }
    for (const match in this.file_contents.matchAll(this.regexp_b)) {
      matches[match[0]] = match[1];
    }
  }
}

function getStyles() {
  const matches = {};
  try {

    let data = fs.readFileSync('./app/views/style/Styles.java', 'utf8');
    let stylesReader = new StylesJavaReader(data);
    
    stylesReader.getMatches(matches);
  }

  catch (error) {
    throw 'error reading Styles.java for tailwindcss processing: ' + error.message;
  }

  return matches;
}

var PROCESSED_TS = false;

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

        const styleDict = getStyles();
        var output = []
        let matchIter = content.match(/(?<=Styles\.)([0-9A-Z_]+)/g);
        console.log(matchIter);
        if (matchIter) {
          for (const matches of matchIter) {
            //console.log(matches)
            for (const m of matches) {
              let s = styleDict[m];
              //console.log(m);
              output.push(s)
              // We don't know which, if any, of these prefixes are in use for any class in particular.
              // We therefore have to use every combination of them.
              for (const [key, value] of Object.entries(prefixes)) {
                output.push(key + ':' + s)
              }
            }
          }
        }

        /*for (const match of content.matchAll(/"([\w-/:]+)"/g)) {
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
        }*/

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

