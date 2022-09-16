module.exports = {
  content: [
    './app/assets/javascripts/*.ts',
    './app/views/style/Styles.java',
    './app/views/style/BaseStyles.java',
  ],
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
