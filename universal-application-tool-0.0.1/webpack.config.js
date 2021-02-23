const path = require('path');

module.exports = {
  entry: "./app",
  output: {
    path: path.resolve(__dirname, 'public/javascripts/'),
    publicPath: '/javascripts/',
    filename: 'bundle.js',
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
