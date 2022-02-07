module.exports = {
  purge: {
    enabled: true,
    content: [
      './app/assets/javascripts/*.ts',
      //'./app/views/style/Styles.java',
      './app/views/style/Styles.java',
      './app/views/style/BaseStyles.java',
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
        for (const match of content.matchAll(/"([\w-/:]+)"/g)) {
          output.push(match[1])
          // We don't know which, if any, of these prefixes are in use for any class in particular.
          // We therefore have to use every combination of them.
          for (const [key, value] of Object.entries(prefixes)) {
            output.push(key + ':' + match[1])
          }
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
