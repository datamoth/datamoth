import * as EV from '../../api/base'

const initialState = {
	name: ""
	, failure: false
	, loading: false
	, authenticated: true
	, errormsg: ""
}

function authReducer(state = initialState, action) {
	switch(action.type) {
		case EV.DYNO_WHOAMI_WAIT:
			return Object.assign({}, state, {
				loading: true
			})
		case EV.DYNO_WHOAMI_SUCC:
			if (action.result.data.length > 0) {
				return Object.assign({}, state, {
					name: action.result.data
					, loading: false
					, failure: false
					, authenticated: true
				})
			}
			return state
		case EV.DYNO_WHOAMI_FAIL:
			return Object.assign({}, state, {
				name: ""
				, loading: false
				, failure: false
				, authenticated: false
			})
		case EV.DYNO_AUTHENTICATE_WAIT:
			return Object.assign({}, state, {
				loading: true
			})
		case EV.DYNO_AUTHENTICATE_SUCC:
			return Object.assign({}, state, {
				name: action.result.data
				, loading: false
				, failure: false
				, authenticated: true
			})
		case EV.DYNO_AUTHENTICATE_FAIL:
			return Object.assign({}, state, {
				loading: false
				, failure: true
				, authenticated: false
			})
		case EV.DYNO_UNAUTHENTICATE_WAIT:
			return Object.assign({}, state, {
				loading: true
			})
		case EV.DYNO_UNAUTHENTICATE_SUCC:
			return Object.assign({}, state, {
				name: ""
				, loading: false
				, failure: false
				, authenticated: false
			})
		case EV.DYNO_UNAUTHENTICATE_FAIL:
			return Object.assign({}, state, {
				name: ""
				, loading: false
				, failure: false
				, authenticated: false
			})
	}
	return state
}

export default authReducer
