const path = require('path');

module.exports = {
  entry: "./app",
  output: {
    filename: 'bundle.js',
    path: path.resolve(__dirname, 'public/assets/javascripts/'),
    publicPath: '/assets/javascripts/',
  },
  module: {
    rules: [
      {
        test: /\.css$/i,
        use: [
          "style-loader",
          "css-loader",
          "postcss-loader"
        ],
      },
    ],
  },
};
