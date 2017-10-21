const path = require("path");

module.exports = function() {
	return {
		context: path.resolve(__dirname, "src"),
		entry: {
			app: "appldr"
		},
		output: {
			path: path.join(__dirname, 'release'),
			filename: '[name].js',
			sourceMapFilename: '[name].map'
		},
		resolve: {
			extensions: ['.ts', '.js', '.json', '.jsx', '.scss', '.css'],
			modules: [path.join(__dirname, 'src'), 'node_modules']
		},
		module: {
			rules: [
				{
					test: /\.jsx?$/,
					exclude: /(node_modules|bower_components|public\/)/,
					loader: "babel-loader",
					query: {
						plugins: [ 'react-hot-loader/babel', 'transform-class-properties' ],
						cacheDirectory: true,
						presets: [ 'es2015', 'react' ]
					}
				}, {
					test: /\.css$/,
					use: [ "style-loader", "css-loader" ]
				}, {
					test: /\.scss$/,
					use: [ "style-loader", "css-loader", "sass-loader" ]
				}
			]
		},
		plugins: [
		]
	};
};
