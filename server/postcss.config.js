/* This file configures the post-css Webpack loader to add vendor prefixes to CSS styles.
This ensures browser compatibility, which USWDS relies on.
*/

module.exports = {
  plugins: [require('autoprefixer')],
}
