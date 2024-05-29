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
        'civiform-blue': {
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
        'civiform-yellow': {
          light: '#ffefc9',
          DEFAULT: '#6b4b00',
        },
        'civiform-teal': {
          DEFAULT: '#164e63',
        },
      },
      spacing: {
        4.5: '1.125rem',
        11.5: '2.875rem',
      },
    },
  },
  plugins: [
    require('@tailwindcss/line-clamp'),

    function ({ matchVariant }) {
      matchVariant(
        "has",
        (value) => {
          return `&:has(${value})`;
        },
        {
          values: {
            checked: "input:checked",
          },
        }
      );
    },
  ]
}
