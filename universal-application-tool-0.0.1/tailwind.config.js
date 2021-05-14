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
        civiformBlue: {
          60: '#B3D1E3',
          80: '#72ACDB',
          100: '#1D5497',
        },
        seattleBlue: {
          DEFAULT: '#113F9F',
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

