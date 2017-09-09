var path = require("path");
var webpack = require("webpack");
var HtmlWebpackPlugin = require("html-webpack-plugin");
const webpackMerge = require('webpack-merge');
const commonConfig = require('./webpack.base.js');

const HOST = "localhost"
const PORT = "8888"

const GLOBALS = {
	'process.env': {
		'NODE_ENV': JSON.stringify('development')
	},
	MATRIX_ENTRANCE: '"http://localhost:2718"',
	__DEV__: JSON.stringify(JSON.parse(process.env.DEBUG || 'true'))
};

module.exports = function(env) {
	return webpackMerge(commonConfig(), {
		cache: true,
		devtool: 'cheap-module-eval-source-map',
		entry: {
			app: [
				'react-hot-loader/patch',
				'appldr'
			]
		},
		output: {
			path: path.join(__dirname, 'release'),
			filename: '[name].js',
			sourceMapFilename: '[name].map'
		},
        plugins: [
            new webpack.HotModuleReplacementPlugin(),
            new webpack.NamedModulesPlugin(),
			new webpack.DefinePlugin(GLOBALS),
			new HtmlWebpackPlugin({
				template: 'template.html',
				inject: false,
				chunksSortMode: 'dependency'
			})
        ],
		devServer: {
			hot: true,
			inline: true,
			historyApiFallback: true,
			port: PORT,
			host: HOST,
			headers: {
				"Access-Control-Allow-Origin": "*"
			}
		}
    })
};
