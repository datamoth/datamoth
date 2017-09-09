import _ from 'lodash'
import React from 'react'
import {
	Label, Table, Input, Accordion, Icon, Feed, Divider
	, List, Card, Container, Grid
	, Image, Button, Dropdown, Segment, Header, Checkbox
	, Message, Dimmer, Menu, Sidebar
} from 'semantic-ui-react'
import { Link } from 'react-router'
import { connect } from 'react-redux'
import Select from 'react-select'
import AceEditor from 'react-ace'
import Measure from 'react-measure'
import Calendar from 'react-calendar-timeline/lib'
import moment from 'moment'

import api from '../../../api'
import styles from './styles.scss';

function status(status, size) {
	const sz = size || "mini"
	switch (status) {
	case "PREP"			: return <Label size={sz} color="orange">{status}</Label>
	case "RUNNING"		: return <Label size={sz} color="orange">{status}</Label>
	case "SUCCEEDED"	: return <Label size={sz} color="green">{status}</Label>
	case "KILLED"		: return <Label size={sz} color="red">{status}</Label>
	case "READY"		: return <Label size={sz} color="orange">{status}</Label>
	case "FAILED"		: return <Label size={sz} color="red">{status}</Label>
	case "SUSPENDED"	: return <Label size={sz} color="orange">{status}</Label>
	default				: return <Label size={sz} color="orange">UNKNOWN</Label>
	}
}

class Timeline extends React.Component {

	constructor(props) {
		super(props)
		this.state = {
			dims: { width: 1, height: 1 }
			, messageVisible: false
			, jobsCount: 1000
		}
	}

	hideMessage() {
		this.setState({
			messageVisible: false
		})
	}

	setDims(dims) {
		this.setState({ dims })
	}

	getJobs(maxCount) {
		const ns = this.props.activeNamespace.name
		const pr = this.props.activeProject.name
		const rf = this.props.activeRef.name
		const pf = this.props.activeProfile.name
		this.props.dispatch(api.getOozieJobs(ns, pr, rf, pf, maxCount))
		this.setState({ jobsCount: maxCount })
	}

	showHelp() { this.setState({ messageVisible: true }) }
	hideHelp() { this.setState({ messageVisible: false }) }

	render() {
		const jobs = this.props.oozieJobs.items
		const groups = Object.keys(_.groupBy(jobs, "appName")).map((name, i) => ({ id: name, title: name }))
		const items = jobs.map((job, i) => ({
			id: i
			, group: job.appName
			, title: job.run
			, start_time: job.startTime
			, end_time: job.endTime
			, canMove: false
			, canResize: false
			, canChangeGroup: false
			, className: job.status
		}))

		return (
			<Container fluid style={{ marginTop: "60px" }}>
				{ this.props.oozieJobs.waiting &&
					<Dimmer inverted page active={true} >
						<Header as='h2' icon >
							<Icon name='snowflake outline' loading />
							Loading jobs
							<Header.Subheader>Please, wait</Header.Subheader>
						</Header>
					</Dimmer>
				}
				{ !this.props.oozieJobs.waiting &&
					<Grid padded stackable >
						<Grid.Column width={16}>
							<Menu color="blue">
								{ [ 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000 ].map(cnt =>
									<Menu.Item key={cnt} onClick={this.getJobs.bind(this, cnt)} active={this.state.jobsCount === cnt}>{cnt}</Menu.Item>
								)}
								{ !this.state.messageVisible && <Menu.Item onClick={::this.showHelp}>Show help</Menu.Item> }
								{ this.state.messageVisible && <Menu.Item onClick={::this.hideHelp}>Hide help</Menu.Item> }
							</Menu>
							{ this.state.messageVisible &&
								<Message onDismiss={::this.hideMessage}>
									Click a grey header to zoom in time. Click a red header to zoom out.
								</Message>
							}
							<Calendar
								groups={groups}
								items={items}
								defaultTimeStart={moment().add(-2, 'month')}
								defaultTimeEnd={moment().add(2, 'month')}
								leftSidebarWidth={400}
							/>
						</Grid.Column>
					</Grid>
				}
			</Container>
		)
	}

}

const empty = { name: "" }
function mapper(state, props) {
	return {
		oozieJobs: state.oozieJobs
		, activeNamespace: state.namespaces.list[state.currentNamespace] || empty
		, activeProject: state.projects.list[state.currentProject] || empty
		, activeRef: state.branches.list[state.currentBranch] || empty
		, activeProfile: state.profiles.list[state.currentProfile] || empty
	}
}

export default connect(mapper)(Timeline)
