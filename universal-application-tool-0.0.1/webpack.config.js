const path = require('path');
const tailwindcss = require('tailwindcss');

module.exports = {
  entry: "./app/index.js",
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
          {
            loader: "postcss-loader",
            options: {
              postcssOptions: {
                plugins: [
                  tailwindcss
                ],
              },
            },
          },
        ],
      },
    ],
  },
};
