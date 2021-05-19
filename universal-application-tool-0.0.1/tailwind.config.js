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
        civiformWhite: {
          DEFAULT: '#f8f9fa',
        },
        seattleBlue: {
          DEFAULT: '#113f9f',
        },
      },
    },
  },
  variants: {
    extend: {
      opacity: ['disabled'],
    }
  },
  plugins: [
    require('@tailwindcss/line-clamp'),
  ],
}

