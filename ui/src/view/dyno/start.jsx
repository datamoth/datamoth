import React from 'react'
import {
	Label, Table, Input, Accordion, Icon, Feed
	, List, Card, Container, Grid 
	, Image, Button, Dropdown, Segment, Header, Checkbox
	, Message, Dimmer, Sidebar, Menu
} from 'semantic-ui-react'
import { Link } from 'react-router';
import { connect } from 'react-redux'


import Login from './auth/login'
import Walker from './walker'
import Timeline from './timeline'
import Navigator from './navi'
import Deploy from './deploy'
import Graph from './graph'
import Help from './help'


class Start extends React.Component {

	constructor(props) {
		super(props)
		this.state = {
			item: localStorage.getItem("datamot-main-page") || "timeline"
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
					showDeploy={::this.showDeploy}
					showWalker={::this.showWalker}
					showTimeline={::this.showTimeline}
					/>
				{ (this.state.item === "deploy") && <Deploy /> }
				{ (this.state.item === "walker") && <Walker /> }
				{ (this.state.item === "timeline") && <Timeline /> }
			</Container>
		)
	}

}


export default Start
