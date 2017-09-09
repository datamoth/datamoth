import { combineReducers } from 'redux'
import * as EV from '../../api/base'
import authReducer from './auth.jsx'


const create = (data) => Object.assign(data, {
	loading: false
	, failure: false
})

const app = {
	namespaces: create({ list: [] })
	, projects: create({ list: [] })
	, branches: create({ list: [] })
	, profiles: create({ list: [] })
	, analysis: create({
		data: {
			coordinators: []
			, databundles: []
			, datasets: []
			, deploy: {}
			, errors: []
			, meta: {}
			, workflows: []
			, deploy: {
				done: true
				, commands: []
				, errors: []
				, oldErrors: []
			}
		}
		, loading: false
		, failure: false
	})
	, commits: create({ list: [] })
	, currentNamespace: -1
	, currentProject: -1
	, currentBranch: -1
	, currentProfile: -1
	, currentCoordinator: { exists: false, waiting: false }
	, currentDataset: { exists: false, waiting: false, items: [] }
	, graph: { visible: false }
	, help: { visible: false }
	, editor: []
	, oozieJobs: { exists: false, waiting: false, items: [] }
}


const createListReducer = (name, field) => {
	return function (state = app[field], action) {
		switch(action.type) {
			case EV["DYNO_" + name  + "_LIST_WAIT"]:
				return Object.assign({}, state, {
					loading: true
					, failure: false
				})
			case EV["DYNO_" + name  + "_LIST_SUCC"]:
				return Object.assign({}, state, {
					list: action.result.data
					, loading: false
					, failure: false
				})
			case EV["DYNO_" + name  + "_LIST_FAIL"]:
				return Object.assign({}, state, {
					list: []
					, loading: false
					, failure: true
				})
		}
		return state
	}
}

const createOpenReducer = (name, field) => {
	return function (state = app[field], action) {
		switch(action.type) {
			case EV["DYNO_OPEN_" + name  + "_WAIT"]:
				return -1
			case EV["DYNO_OPEN_" + name  + "_SUCC"]:
				return action.result.data
			case EV["DYNO_OPEN_" + name  + "_FAIL"]:
				return -1
		}
		return state
	}
}

const analysisReducer = (state = app.analysis, action) => {
	switch(action.type) {
		case EV.DYNO_COMPILE_WAIT:
		case EV.DYNO_DEPLOY_WAIT:
			return Object.assign({}, state, {
				loading: true
			})
		case EV.DYNO_COMPILE_SUCC:
		case EV.DYNO_DEPLOY_SUCC:
			return Object.assign({}, state, {
				data: action.result.data
				, loading: false
				, failure: false
			})
		case EV.DYNO_COMPILE_FAIL:
		case EV.DYNO_DEPLOY_FAIL:
			return Object.assign({}, state, {
				data: {
					coordinators: []
					, databundles: []
					, datasets: []
					, deploy: {}
					, errors: []
					, meta: {}
					, workflows: []
					, deploy: {
						commands: []
						, errors: []
						, oldErrors: []
					}
				}
				, loading: false
				, failure: true
			})
	}
	return state
}

const graphReducer = (state = app.graph, action) => {
	switch(action.type) {
		case EV.DYNO_SHOW_GRAPH: return Object.assign({}, state, { visible: true })
		case EV.DYNO_HIDE_GRAPH: return Object.assign({}, state, { visible: false })
	}
	return state
}

const editorReducer = (state = app.editor, action) => {
	switch(action.type) {
		case EV.DYNO_GETFILE_WAIT:
			return {}
		case EV.DYNO_GETFILE_SUCC:
			return action.result.data
	}
	return state
}


const helpReducer = (state = app.help, action) => {
	switch(action.type) {
		case EV.DYNO_SHOW_HELP: return Object.assign({}, state, { visible: true })
		case EV.DYNO_HIDE_HELP: return Object.assign({}, state, { visible: false })
	}
	return state
}

const coordinatorReducer = (state = app.currentCoordinator, action) => {
	switch(action.type) {
		case EV.DYNO_GET_OOZIE_COORDINATOR_INFO_WAIT:
			return { exists: false, waiting: true }
		case EV.DYNO_GET_OOZIE_COORDINATOR_INFO_SUCC:
			if (action.result.data.length === 0) {
				return { exists: false, waiting: false }
			} else {
				return Object.assign({ exists: true, waiting: false }, action.result.data[0])
			}
	}
	return state
}

const oozieJobsReducer = (state = app.oozieJobs, action) => {
	switch(action.type) {
		case EV.DYNO_GET_OOZIE_JOBS_WAIT:
			return { exists: false, waiting: true, items: [] }
		case EV.DYNO_GET_OOZIE_JOBS_FAIL:
			return { exists: false, waiting: false, items: [] }
		case EV.DYNO_GET_OOZIE_JOBS_SUCC:
			if (action.result.data.length === 0) {
				return { exists: false, waiting: false }
			} else {
				return { exists: true, waiting: false, items: action.result.data }
			}
	}
	return state
}

const datasetReducer = (state = app.currentDataset, action) => {
	switch(action.type) {
		case EV.DYNO_HDFS_LS_WAIT:
			return { exists: false, waiting: true, size: -1, space: -1 }
		case EV.DYNO_HDFS_LS_FAIL:
			return { exists: false, waiting: false, size: -1, space: -1  }
		case EV.DYNO_HDFS_LS_SUCC:
			const list = action.result.data
			const size = list.reduce((t, n) => t + n.size, 0)
			const space = list.reduce((t, n) => t + n.space, 0)
			return { exists: true, waiting: false, size: size, space: space, items: action.result.data }
	}
	return state
}

export default combineReducers({
	auth: authReducer
	, namespaces: createListReducer("NAMESPACE", "namespaces")
	, projects: createListReducer("PROJECT", "projects")
	, branches: createListReducer("BRANCH", "branches")
	, profiles: createListReducer("PROFILE", "profiles")
	, commits: createListReducer("COMMIT", "commits")
	, analysis: analysisReducer
	, currentNamespace: createOpenReducer("NAMESPACE", "currentNamespace")
	, currentProject: createOpenReducer("PROJECT", "currentProject")
	, currentBranch: createOpenReducer("BRANCH", "currentBranch")
	, currentProfile: createOpenReducer("PROFILE", "currentProfile")
	, graph: graphReducer
	, help: helpReducer
	, editor: editorReducer
	, currentCoordinator: coordinatorReducer
	, currentDataset: datasetReducer
	, oozieJobs: oozieJobsReducer
})
