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
      backgroundColor: ['disabled'],
      textColor: ['disabled'],
      opacity: ['disabled'],
    }
  },
  plugins: [],
}

