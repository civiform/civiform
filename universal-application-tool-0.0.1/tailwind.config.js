module.exports = {
  purge: [
    './**/*.html',
    './**/*.js',
  ],
  darkMode: false,  // or 'media' or 'class'
  theme: {
    extend: {},
  },
  variants: {
    extend: {
      backgroundColor: ['even'],
    }
  },
  plugins: [],
}

