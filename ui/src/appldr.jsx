import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import { AppContainer } from 'react-hot-loader';

import Store from './store';
import Router from './router';

const render = (Component) => {
	ReactDOM.render(
		<AppContainer><Provider store={Store}>{Component}</Provider></AppContainer>,
		document.querySelector("#app")
	);
}

render(Router);

if (module.hot) {
	module.hot.accept('./router', () => {
		render(Router)
	})
}
