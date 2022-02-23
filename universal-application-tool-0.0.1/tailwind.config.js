module.exports = {
  purge: {
    enabled: true,
    content: [
      './app/assets/javascripts/*.ts',
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
        var output = []
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
