import React from 'react'
import { Container } from 'semantic-ui-react'

import Login from './auth/login'
import Walker from './walker'
import Timeline from './timeline'
import Navigator from './navi'
import Deploy from './deploy'

class Start extends React.Component {

	constructor(props) {
		super(props)
		this.state = {
			item: localStorage.getItem("datamot-main-page") || "timeline"
		}
		this.showDeploy = this.showDeploy.bind(this)
		this.showWalker = this.showWalker.bind(this)
		this.showTimeline = this.showTimeline.bind(this)
	}

	get content() {
		switch(this.state.item) {
			case 'deploy':
				return <Deploy />
			case 'walker':
				return <Walker />
			case 'timeline':
				return <Timeline />
		}
	}

	showDeploy() {
		localStorage.setItem("datamot-main-page", "deploy")
		this.setState({ item: "deploy" })
	}

	showWalker() {
		localStorage.setItem("datamot-main-page", "walker")
		this.setState({ item: "walker" })
	}

	showTimeline() {
		localStorage.setItem("datamot-main-page", "timeline")
		this.setState({ item: "timeline" })
	}

	render() {
		return (
			<Container fluid >
				<Login />
				<Navigator
					showDeploy={this.showDeploy}
					showWalker={this.showWalker}
					showTimeline={this.showTimeline}
					/>
				{this.content}
			</Container>
		)
	}

}

export default Start
