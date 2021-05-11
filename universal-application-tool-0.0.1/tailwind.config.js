const colors = require('tailwindcss/colors')

module.exports = {
  purge: [
    './**/*.html',
    './**/*.js',
  ],
  darkMode: false,  // or 'media' or 'class'
  theme: {
    extend: {
      colors: {
        orange: colors.orange,
        teal: colors.teal,
        beige: {
          DEFAULT: "#eeede8",
        },
      },
    },
  },
  variants: {
    extend: {
      opacity: ['disabled'],
    }
  },
  plugins: [],
}

