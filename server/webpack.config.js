const MiniCssExtractPlugin = require('mini-css-extract-plugin')

module.exports = {
  mode: 'production',
  devtool: 'source-map',
  stats: 'errors-only',
  module: {
    rules: [
      {
        test: /\.scss$/,
        /* The Sass compilation is only for USWDS currently.  Loaders run from
        the bottom up. All of these loaders are 3rd-party packages which don't
        come with Webpack. That's why they are dependencies in package.json. */
        use: [
          MiniCssExtractPlugin.loader /* Puts the CSS string into a file. */,
          'css-loader' /* Collects all the CSS which resulted from the
                        Sass compilation below and puts it into a single string. */,
          'postcss-loader' /* Adds vendor prefixes (like -webkit-, -moz- or -ms-)
                            so that styles are compatible with all browsers. */,
          {
            loader: 'sass-loader' /* Converts Sass into CSS. */,
            options: {
              sassOptions: {
                includePaths: ['./node_modules/@uswds/uswds/packages'],
              },
            },
          },
        ],
      },
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules\/(?!(@uswds)\/).*/,
      },
    ],
  },
  resolve: {
    extensions: ['.ts', '.js'],
  },
  entry: {
    applicant: './app/assets/javascripts/applicant_entry_point.ts',
    admin: './app/assets/javascripts/admin_entry_point.ts',
    uswds: './node_modules/@uswds/uswds/dist/js/uswds.min.js',
    uswdsStyles: './app/assets/stylesheets/uswds/styles.scss',
  },
  output: {
    filename: `[name].bundle.js`,
    sourceMapFilename: '[name].bundle.js.map',
    iife: true,
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: 'uswds.min.css' /* The name for the compiled output CSS file */,
    }),
  ],
}
