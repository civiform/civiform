function pushWithPrefixes(output, match) {
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

module.exports = {
  content: {
    files: ['./app/assets/javascripts/*.ts', './app/views/**/*.java'],
    // Override tailwind's default extractor in order to include style prefixes
    // since we generate those dynamically. See:
    //  https://tailwindcss.com/docs/content-configuration#customizing-extraction-logic
    extract: {
      java: (content) => {
        const output = []
        for (const match of content.matchAll(/"([\w-/:.]+)"/g)) {
          pushWithPrefixes(output, match)
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
      spacing: {
        4.5: '18px',
      },
    },
  },
  plugins: [require('@tailwindcss/line-clamp')],
}
