import React from 'react'
import { Menu, Label, Icon, Popup } from 'semantic-ui-react'
import { Link } from 'react-router'
import { connect } from 'react-redux'
import CopyToClipboard from 'react-copy-to-clipboard'

import api from '../../../api'
import NavBranches from './navbranches'
import NavProfiles from './navprofiles'

class Navigator extends React.Component {

	leave() {
		this.props.dispatch(api.leave())
	}

	refresh(ns, pr, br, pf) {
		this.props.dispatch(api.compile(ns, pr, br, pf))
	}

	render() {
		const ns = this.props.namespace.name
		const pr = this.props.project.name
		const brpath = this.props.branch.name.split("/")
		const br = brpath[brpath.length - 1]
		const pf = this.props.profile.name
		const waiting = (ns === "" || pr === "")
		return (
			<Menu size='tiny' fixed='top' inverted color='blue' >
				<Menu.Item header >
					{ns} / {pr}
				</Menu.Item>
				{this.props.children}
				<Menu.Item icon='fa-code-fork' color='blue' ></Menu.Item>
				<NavBranches />
				<Menu.Item icon='options'></Menu.Item>
				<NavProfiles />

				<Popup trigger={
					<Menu.Item icon='upload' as={Link} onClick={this.props.showDeploy} >
					</Menu.Item>
				} inverted content='Deploy page' />

				<Popup trigger={
					<Menu.Item icon='sitemap' as={Link} onClick={this.props.showWalker} >
					</Menu.Item>
				} inverted content='Jobs & Datasets walker' />

				<Popup trigger={
					<Menu.Item icon='tasks' as={Link} onClick={this.props.showTimeline} >
					</Menu.Item>
				} inverted content='Jobs timeline' />

				<Popup trigger={
					<Menu.Item as={Link} >
						<CopyToClipboard text={this.props.project.remote} >
							<Label color="black" size="medium" >
								<Icon name='git' />
								<Label.Detail>{this.props.project.remote}</Label.Detail>
							</Label>
						</CopyToClipboard>
					</Menu.Item>
				} inverted content='Click to copy to clipboard' />

				<Menu.Menu position="right">
					<Menu.Item >{this.props.user}</Menu.Item>
					<Menu.Item name='leave' as={Link} onClick={::this.leave}>
						:=->
					</Menu.Item>
				</Menu.Menu>
			</Menu>
		)
	}
}

const empty = { name: "" }

function mapper(state, props) {
	return {
		project: state.projects.list[state.currentProject] || empty
		, namespace: state.namespaces.list[state.currentNamespace] || empty
		, branch: state.branches.list[state.currentBranch] || empty
		, profile: state.profiles.list[state.currentProfile] || empty
		, user: state.auth.name
	}
}

export default connect(mapper)(Navigator)
