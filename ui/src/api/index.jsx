import axios from 'axios'
import Promise from 'bluebird'

import * as EV from './base'


const MATRIX_BASE_URL = EV.MATRIX_BASE_URL

function match_event(cmd) {
	switch (cmd) {
		case "list-namespaces"	: return EV.DYNO_NAMESPACE_LIST_SUCC
		case "list-projects"	: return EV.DYNO_PROJECT_LIST_SUCC
		case "list-branches"	: return EV.DYNO_BRANCH_LIST_SUCC
		case "list-profiles"	: return EV.DYNO_PROFILE_LIST_SUCC
		case "commit-log"		: return EV.DYNO_COMMIT_LIST_SUCC
		case "open-namespace"	: return EV.DYNO_OPEN_NAMESPACE_SUCC
		case "open-project"		: return EV.DYNO_OPEN_PROJECT_SUCC
		case "open-branch"		: return EV.DYNO_OPEN_BRANCH_SUCC
		case "open-profile"		: return EV.DYNO_OPEN_PROFILE_SUCC
		case "compile"			: return EV.DYNO_COMPILE_SUCC
		case "deploy"			: return EV.DYNO_DEPLOY_SUCC
		case "reset-poll"		: return EV.DYNO_POLL_RESET_SUCC
	}
}

function whoami() {
	return {
		types: [
			EV.DYNO_WHOAMI_WAIT
			, EV.DYNO_WHOAMI_SUCC
			, EV.DYNO_WHOAMI_FAIL
		]
		, promise: () => {
			return axios.get("/whoami", {
				baseURL: MATRIX_BASE_URL
				, withCredentials: true
				, params: {
				}
			})
		}
	}
}

function enter(name, pass) {
	return {
		types: [
			EV.DYNO_AUTHENTICATE_WAIT
			, EV.DYNO_AUTHENTICATE_SUCC
			, EV.DYNO_AUTHENTICATE_FAIL
		]
		, promise: () => {
			return axios.get("/enterthematrix", {
				baseURL: MATRIX_BASE_URL
				, withCredentials: true
				, params: {
					user: name
					, pass: pass
				}
			})
		}
	}
}

function leave() {
	return {
		types: [
			EV.DYNO_UNAUTHENTICATE_WAIT
			, EV.DYNO_UNAUTHENTICATE_SUCC
			, EV.DYNO_UNAUTHENTICATE_FAIL
		]
		, promise: () => {
			return axios.get("/leavethematrix", {
				baseURL: MATRIX_BASE_URL
				, params: {
				}
			})
		}
	}
}

function namespaces() {
	return {
		types: [
			EV.DYNO_NAMESPACE_LIST_WAIT,
			EV.DYNO_NAMESPACE_LIST_SUCC,
			EV.DYNO_NAMESPACE_LIST_FAIL
		]
		, promise: () => {
			return axios.get("/api/", {
				baseURL: MATRIX_BASE_URL
				, timeout: 20000
				, withCredentials: true
				, params: {
					action: "list-namespaces"
				}
			})
		}
	}
}

function projects() {
	return {
		types: [
			EV.DYNO_PROJECT_LIST_WAIT,
			EV.DYNO_PROJECT_LIST_SUCC,
			EV.DYNO_PROJECT_LIST_FAIL
		]
		, promise: () => {
			return axios.get("/api/", {
				baseURL: MATRIX_BASE_URL
				, timeout: 20000
				, withCredentials: true
				, params: {
					action: "list-projects"
				}
			})
		}
	}
}

function branches(namespace, project) {
	return {
		types: [
			EV.DYNO_BRANCH_LIST_WAIT,
			EV.DYNO_BRANCH_LIST_SUCC,
			EV.DYNO_BRANCH_LIST_FAIL
		]
		, promise: () => {
			return axios.get("/api/" + namespace + "/" + project, {
				baseURL: MATRIX_BASE_URL
				, timeout: 20000
				, withCredentials: true
				, params: {
					action: "list-refs"
				}
			})
		}
	}
}

function profiles(namespace, project, ref) {
	return {
		types: [
			EV.DYNO_PROFILE_LIST_WAIT,
			EV.DYNO_PROFILE_LIST_SUCC,
			EV.DYNO_PROFILE_LIST_FAIL
		]
		, promise: () => {
			return axios.get("/api/" + namespace + "/" + project + "/" + ref, {
				baseURL: MATRIX_BASE_URL
				, timeout: 20000
				, withCredentials: true
				, params: {
					action: "list-profiles"
				}
			})
		}
	}
}

function openNamespace(name) {
	if (typeof name === "string") {
		return {
			type: EV.DYNO_OPEN_NAMESPACE
			, data: name
		}
	} else {
		const idx = arguments[0]
		return {
			types: [
				EV.DYNO_OPEN_NAMESPACE_WAIT,
				EV.DYNO_OPEN_NAMESPACE_SUCC,
				EV.DYNO_OPEN_NAMESPACE_FAIL
			]
			, promise: () => {
				return new Promise((resolve, reject) => {
					resolve(EV.succ(idx))
				})
			}
		}
	}
}

function openProject(namespace, name) {
	if (typeof namespace === "string") {
		return {
			type: EV.DYNO_OPEN_PROJECT
			, data: { namespace: namespace, project: name }
		}
	} else {
		const idx = arguments[0]
		return {
			types: [
				EV.DYNO_OPEN_PROJECT_WAIT,
				EV.DYNO_OPEN_PROJECT_SUCC,
				EV.DYNO_OPEN_PROJECT_FAIL
			]
			, promise: () => {
				return new Promise((resolve, reject) => {
					resolve(EV.succ(idx))
				})
			}
		}
	}
}

function openBranch(namespace, project, branch) {
	if (typeof namespace === "string") {
		return {
			type: EV.DYNO_OPEN_BRANCH
			, data: { namespace: namespace, project: project, branch: branch }
		}
	} else {
		const idx = arguments[0]
		return {
			types: [
				EV.DYNO_OPEN_BRANCH_WAIT,
				EV.DYNO_OPEN_BRANCH_SUCC,
				EV.DYNO_OPEN_BRANCH_FAIL
			]
			, promise: () => {
				return new Promise((resolve, reject) => {
					setTimeout(() => resolve(EV.succ(idx)), 0)
				})
			}
		}
	}
}

function openProfile(namespace, project, branch, profile) {
	if (typeof namespace === "string") {
		return {
			type: EV.DYNO_OPEN_PROFILE
			, data: { namespace: namespace, project: project, branch: branch, profile: profile }
		}
	} else {
		const idx = arguments[0]
		return {
			types: [
				EV.DYNO_OPEN_PROFILE_WAIT,
				EV.DYNO_OPEN_PROFILE_SUCC,
				EV.DYNO_OPEN_PROFILE_FAIL
			]
			, promise: () => {
				return new Promise((resolve, reject) => {
					resolve(EV.succ(idx))
				})
			}
		}
	}
}

function compile(namespace, project, ref, profile) {
	return {
		types: [
			EV.DYNO_COMPILE_WAIT,
			EV.DYNO_COMPILE_SUCC,
			EV.DYNO_COMPILE_FAIL
		]
		, promise: () => {
			return axios.get("/api/" + namespace + "/" + project + "/" + ref + "/" + profile, {
				baseURL: MATRIX_BASE_URL
				, timeout: 20000
				, withCredentials: true
				, params: {
					action: "get-project"
				}
			})
		}
	}
}

function commits(namespace, project, ref) {
	return {
		types: [
			EV.DYNO_COMMIT_LIST_WAIT,
			EV.DYNO_COMMIT_LIST_SUCC,
			EV.DYNO_COMMIT_LIST_FAIL
		]
		, promise: () => {
			return axios.get("/api/" + namespace + "/" + project + "/" + ref, {
				baseURL: MATRIX_BASE_URL
				, timeout: 20000
				, withCredentials: true
				, params: {
					action: "list-commits"
				}
			})
		}
	}
}

function showGraph() {
	return {
		type: EV.DYNO_SHOW_GRAPH
		, data: {}
	}
}
function hideGraph() {
	return {
		type: EV.DYNO_HIDE_GRAPH
		, data: {}
	}
}

function showHelp() {
	return {
		type: EV.DYNO_SHOW_HELP
		, data: {}
	}
}
function hideHelp() {
	return {
		type: EV.DYNO_HIDE_HELP
		, data: {}
	}
}


function getFiles(namespace, project, ref, profile, paths) {
	return {
		types: [
			EV.DYNO_GETFILE_WAIT,
			EV.DYNO_GETFILE_SUCC,
			EV.DYNO_GETFILE_FAIL
		]
		, promise: () => {
			const qs = "?" + paths.map(p => "paths=" + encodeURIComponent(p)).join("&") + "&action=get-files"
			return axios.get("/api/" + namespace + "/" + project + "/" + ref + "/" + profile + qs, {
				baseURL: MATRIX_BASE_URL
				, timeout: 50000
				, withCredentials: true
			})
		}
	}
}

function getOozieCoordinatorInfo(namespace, project, ref, profile, cname) {
	return {
		types: [
			EV.DYNO_GET_OOZIE_COORDINATOR_INFO_WAIT,
			EV.DYNO_GET_OOZIE_COORDINATOR_INFO_SUCC,
			EV.DYNO_GET_OOZIE_COORDINATOR_INFO_FAIL
		]
		, promise: () => {
			return axios.get("/api/" + namespace + "/" + project + "/" + ref + "/" + profile, {
				baseURL: MATRIX_BASE_URL
				, timeout: 20000
				, withCredentials: true
				, params: {
					action: "get-oozie-coordinator-info"
					, name: cname
				}
			})
		}
	}
}

function getOozieJobs(namespace, project, ref, profile, maxCount) {
	return {
		types: [
			EV.DYNO_GET_OOZIE_JOBS_WAIT,
			EV.DYNO_GET_OOZIE_JOBS_SUCC,
			EV.DYNO_GET_OOZIE_JOBS_FAIL
		]
		, promise: () => {
			return axios.get("/api/" + namespace + "/" + project + "/" + ref + "/" + profile, {
				baseURL: MATRIX_BASE_URL
				, timeout: 20000
				, withCredentials: true
				, params: {
					action: "get-oozie-jobs"
					, "max-count": maxCount
				}
			})
		}
	}
}

function hdfsLs(namespace, project, ref, profile, path) {
	return {
		types: [
			EV.DYNO_HDFS_LS_WAIT,
			EV.DYNO_HDFS_LS_SUCC,
			EV.DYNO_HDFS_LS_FAIL
		]
		, promise: () => {
			return axios.get("/api/" + namespace + "/" + project + "/" + ref + "/" + profile, {
				baseURL: MATRIX_BASE_URL
				, timeout: 20000
				, withCredentials: true
				, params: {
					action: "hdfs-ls"
					, path: path
				}
			})
		}
	}
}

export default {
	whoami: whoami
	, enter: enter
	, leave: leave
	, namespaces: namespaces
	, projects: projects
	, branches: branches
	, profiles: profiles
	, commits: commits
	, compile: compile
	, openNamespace: openNamespace
	, openProject: openProject
	, openBranch: openBranch
	, openProfile: openProfile
	, showGraph: showGraph
	, hideGraph: hideGraph
	, showHelp: showHelp
	, hideHelp: hideHelp
	, getFiles: getFiles
	, match_event: match_event
	, getOozieCoordinatorInfo: getOozieCoordinatorInfo
	, getOozieJobs: getOozieJobs
	, hdfsLs: hdfsLs
}
