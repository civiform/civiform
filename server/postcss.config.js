/* This file configures the post-css loader to add vendor prefixes to CSS styles.
This ensures browser compatibility, which USWDS relies on.
*/

module.exports = {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
