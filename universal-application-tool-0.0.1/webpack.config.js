const path = require('path');
const tailwindcss = require('tailwindcss');

const postcssLoader = {
  loader: "postcss-loader",
  options: {
    postcssOptions: {
      plugins: [tailwindcss],
    },
  },
};

module.exports = {
  entry: "./app/index.js",
  output: {
    path: path.resolve(__dirname, 'public/javascripts'),
    publicPath: '/javascripts',
    filename: 'bundle.js',
  },
  module: {
    rules: [
      {
        test: /\.css$/i,
        use: [
          "style-loader",
          "css-loader",
          postcssLoader,
        ],
      },
    ],
  },
};
