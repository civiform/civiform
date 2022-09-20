module.exports = {
  content: {
    files: [
      './app/assets/javascripts/*.ts',
      './app/views/style/Styles.java',
      './app/views/style/BaseStyles.java',
    ],
    // Override tailwind's default extractor in order to include style prefixes
    // since we generate those dynamically. See:
    //  https://tailwindcss.com/docs/content-configuration#customizing-extraction-logic
    //
    // However, this results in a very large 3.1MB CSS file so we'll need to convert to
    // style literals to use the default extractor (as a separate PR) before this gets
    // pushed to prod. See:
    //  https://github.com/civiform/civiform/pull/2630
    extract: {
      java: (content) => {
        const output = []
        for (const match of content.matchAll(/"([\w-/:.]+)"/g)) {
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
  theme: {
    extend: {
      colors: {
        'civiform-white': {
          DEFAULT: '#f8f9fa',
        },
        'seattle-blue': {
          DEFAULT: '#113f9f',
        },
        'civiform-green': {
          light: '#e8f5e9',
          DEFAULT: '#1b5e20',
        },
        'civiform-purple': {
          light: '#f3e5f5',
          DEFAULT: '#4a148c',
        },
      },
    },
  },
  plugins: [require('@tailwindcss/line-clamp')],
}
