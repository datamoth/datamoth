import _ from 'lodash'
import { put, take, select } from 'redux-saga/effects'

import * as EV from '../../api/base'
import api from '../../api'


function* authenticate() {
	while (true) {
		yield take(EV.DYNO_AUTHENTICATE_SUCC)
		yield put(api.namespaces())
	}
}

function* whoami() {
	while (true) {
		yield take(EV.DYNO_WHOAMI_SUCC)
		yield put(api.namespaces())
	}
}

function* on_project_list() {
	while (true) {
		yield take(EV.DYNO_NAMESPACE_LIST_SUCC)
		yield put(api.projects())
		yield take(EV.DYNO_PROJECT_LIST_SUCC)
		const projects = yield select((state) => state.projects.list)
		if (projects.length > 0) {
			const p = projects[0]
			yield put(api.openProject(p.namespace, p.name))
		}
	}
}

function* on_open_project() {
	while (true) {
		const { data: { namespace: namespace, project: project } } = yield take(EV.DYNO_OPEN_PROJECT)
		const [ namespaces, projects ] = yield select((state) => [ state.namespaces.list, state.projects.list])
		const nidx = _.findIndex(namespaces, (n) => (n.name === namespace))
		const pidx = _.findIndex(projects, (p) => (p.name === project && p.namespace === namespace))
		const p = projects[pidx]
		yield put(api.openNamespace(nidx))
		yield take(EV.DYNO_OPEN_NAMESPACE_SUCC)
		yield put(api.openProject(pidx))
		yield take(EV.DYNO_OPEN_PROJECT_SUCC)
		yield put(api.branches(p.namespace, p.name))
		yield take(EV.DYNO_BRANCH_LIST_SUCC)
		const branches = yield select((state) => state.branches.list)
		if (branches.length > 0) {
			yield put(api.openBranch(0))
		}
	}
}

function* on_open_branch() {
	while (true) {
		yield take(EV.DYNO_OPEN_BRANCH_SUCC)
		const { projects, pi } = yield select((state) => ({ projects: state.projects.list, pi: state.currentProject }))
		const { branches, bi } = yield select((state) => ({ branches: state.branches.list, bi: state.currentBranch }))
		const project = projects[pi]
		const reftemp = branches[bi].name.split("/")
		const ref = reftemp[reftemp.length - 1]
		yield put(api.commits(project.namespace, project.name, ref))
		yield take(EV.DYNO_COMMIT_LIST_WAIT)
		yield put(api.profiles(project.namespace, project.name, ref))
		yield take(EV.DYNO_PROFILE_LIST_SUCC)
		const profiles = yield select((state) => state.profiles.list)
		if (profiles.length > 0) {
			yield put(api.openProfile(0))
		}
	}
}

function* on_open_profile() {
	while (true) {
		yield take(EV.DYNO_OPEN_PROFILE_SUCC)

		const { projects, pi } = yield select((state) => ({ projects: state.projects.list, pi: state.currentProject }))
		const { branches, bi } = yield select((state) => ({ branches: state.branches.list, bi: state.currentBranch }))
		const { profiles, fi } = yield select((state) => ({ profiles: state.profiles.list, fi: state.currentProfile }))

		const project = projects[pi]
		const reftemp = branches[bi].name.split("/")
		const ref = reftemp[reftemp.length - 1]
		const profile = profiles[fi].name

		yield put(api.compile(project.namespace, project.name, ref, profile))
		yield put(api.getOozieJobs(project.namespace, project.name, ref, profile, 1000))

		yield take(EV.DYNO_COMPILE_SUCC)
	}
}

function* root() {
	yield [
		whoami()
		, authenticate()
		, on_project_list()
		, on_open_project()
		, on_open_branch()
		, on_open_profile()
	]
}

export default root
