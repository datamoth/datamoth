import createSagaMiddleware from 'redux-saga'
import { createStore, applyMiddleware } from 'redux'
import { createLogger } from 'redux-logger'

import * as EV from './api/base'
import api from './api'

import rootSaga from './saga/dyno/root'
import rootReducer from './reducer/dyno/root'

function promiseMiddleware() {
	return (next) => (action) => {
		const { promise, types, ...rest } = action  // eslint-disable-line
		if (!promise) {
			return next(action)
		}
		const [REQUEST, SUCCESS, FAILURE] = types
		next({...rest, type: REQUEST})
		return promise().then(
			(result) => {
				if (result.hasOwnProperty("status") && result.hasOwnProperty("statusText")) {
					result = result.data
				}
				if (result.hasOwnProperty("code") && result.code === 0) {
					if (result.data.hasOwnProperty("data")) {
						result = result.data
					}
					next({...rest, result, type: SUCCESS})
				} else {
					next({...rest, result, type: FAILURE})
				}
			},
			(error) => {
				const err = { code: -1, msg: error.message, data: [] }
				next({...rest, result: err, type: FAILURE})
			}
		)
	}
}

const commander = promiseMiddleware
const saga = createSagaMiddleware()
const logger = createLogger()

const Store = applyMiddleware(commander, saga, logger)(createStore)(rootReducer)

saga.run(rootSaga)
Store.dispatch(api.whoami())

export default Store
