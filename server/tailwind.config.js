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
  // Until we fully drop Thymeleaf we need to keep some classes being generated. The files list above
  // scans java (j2html) and typescript files to automatically include them. As files are deleted
  // though we sometimes end up losing classes. Adding html files to the scanner end up missing things
  // or putting them in different order in the css and when there are clashes with USWDS you'll end
  // up with the wrong css getting applied sometimes.
  safelist: [
    '-mt-4',
    'bg-opacity-75',
    'border-4',
    'col-span-5',
    'col-start-1',
    'col-start-8',
    'font-mono',
    'grid-cols-8',
    'grid-rows-4',
    'grid-rows-6',
    'place-content-between',
    'pt-0',
    'px-6',
    'row-span-2',
    'row-start-1',
    'row-start-2',
    'row-start-3',
    'row-start-5',
    'sm:text-5xl',
    'space-x-3',
    'usa-alert-remove-top-margin',
    'w-1/6',
  ],
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
  }
}
