module.exports = {
  mode: 'production',
  devtool: 'source-map',
  stats: 'errors-only',
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/,
      },
    ],
  },
  resolve: {
    extensions: ['.ts', '.js'],
  },
  entry: {
    applicant: './app/assets/javascripts/applicant_entry_point.ts',
    admin: './app/assets/javascripts/admin_entry_point.ts',
  },
  output: {
    filename: `[name].bundle.js`,
    sourceMapFilename: '[name].bundle.js.map',
    iife: true,
  },
}
