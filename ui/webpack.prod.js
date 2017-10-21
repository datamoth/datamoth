const webpack = require("webpack");
const webpackMerge = require('webpack-merge');
const commonConfig = require('./webpack.base.js');
const HtmlWebpackPlugin = require("html-webpack-plugin");

const GLOBALS = {
	'process.env': {
		'NODE_ENV': JSON.stringify('production')
	},
	MATRIX_ENTRANCE: '""',
	__DEV__: JSON.stringify(JSON.parse(process.env.DEBUG || 'false'))
};

module.exports = function(env) {
	return webpackMerge(commonConfig(), {
		devtool: 'cheap-module-source-map',
		entry: {
			app: 'appldr'
		},
		plugins: [
			new HtmlWebpackPlugin({
				inject: false,
				template: 'template.html',
				filename: '../release/index.html',
				chunksSortMode: 'dependency'
			}),
			new webpack.NoEmitOnErrorsPlugin(),
			new webpack.DefinePlugin(GLOBALS),
			new webpack.LoaderOptionsPlugin({
				minimize: true,
				debug: false
			}),
			new webpack.optimize.UglifyJsPlugin({
				beautify: false,
				mangle: {
					screw_ie8: true,
					keep_fnames: true
				},
				compress: {
					screw_ie8: true
				},
				comments: false
			})
		]
	});
};
