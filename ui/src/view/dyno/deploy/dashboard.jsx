import React from 'react'
import {
	Label, Form, Input, Accordion, Icon, Feed
	, List, Menu, Card, Container, Grid, Link
	, Image, Button, Dropdown, Segment, Header, Checkbox
	, Message, Table, Divider
} from 'semantic-ui-react'
import { connect } from 'react-redux'

import { EmojiText } from '../../nice/text'
import api from '../../../api'

const acts = [ 'kill', 'start', 'suspend', 'resume' ]

class Dashboard extends React.Component {

	mtype = {
		"error": { negative: true }
		, "warn": { warning: true }
		, "info": { info: true }
	}

	deploy() {
		const ns = this.props.namespace.name
		const pr = this.props.project.name
		const brpath = this.props.branch.name.split("/")
		const br = brpath[brpath.length-1]
		const fr = this.props.profile.name
	}

	renderAction(action) {
		const cmap = {
			'kill': 'red'
			, 'start': 'green'
			, 'suspend': 'orange'
			, 'resume': 'blue'
		}
		return (
			<Label key={Math.random()} size='tiny' basic color={cmap[action]} >{action}</Label>
		)
	}

	renderActionError(e, i) {
		return (
			<p key={i}>{e.message}</p>
		)
	}

	renderCommand(cmd) {
		return (
			<Table.Row key={cmd.coordinatorName} error={cmd.errors.length > 0}>
				<Table.Cell collapsing ><Label basic size='tiny' color='blue'>C</Label><a href='#'> {cmd.coordinatorName}</a></Table.Cell>
				<Table.Cell collapsing >{cmd.actions.map(::this.renderAction)}</Table.Cell>
				<Table.Cell collapsing >{cmd.errors.map(::this.renderActionError)}</Table.Cell>
			</Table.Row>
		)
	}

	renderDeployError1(e, i) {
		return (
			<Table.Row key={i}>
				<Table.Cell collapsing ><p>{e.message}</p></Table.Cell>
			</Table.Row>
		)
	}

	renderDeployError(e, i) {
		return (
			<Message.Item key={i}>
				{e.message}
			</Message.Item>
		)
	}

	renderCompileError(e, i) {
		return (
			<Message key={i} {...this.mtype[e.kind]} size="mini" >
				<Message.Header>{e.location.file}: {e.location.row}</Message.Header>
				<p>{e.message}</p>
			</Message>
		)
	}

	render() {
		const analysis = this.props.analysis
		const deploy = analysis.deploy
		return (
			<Container fluid={true}>
				<Divider horizontal >DEPLOY STATE</Divider>
				{ ((deploy.errors.length > 0) || (analysis.errors.length > 0)) &&
					<Message color='orange' >
						<Message.Header>
							<EmojiText text=":wrench: There are issues in code, can't deploy" />
						</Message.Header>
						<Message.Content>
							<Message.List>
								{deploy.errors.map(::this.renderDeployError)}
							</Message.List>
						</Message.Content>
					</Message>
				}
				{ deploy.done && (deploy.oldErrors.length > 0) &&
					<Message negative >
						<Message.Header>
						<EmojiText text="There were errors during deploy :warning:" />
						</Message.Header>
						<Message.List>
							{deploy.oldErrors.map(::this.renderDeployError)}
						</Message.List>
					</Message>
				}
				{ (analysis.errors.length > 0) &&
					<Container fluid>
						<Divider clearing horizontal >Compilation errors</Divider>
						{analysis.errors.map(this.renderCompileError.bind(this))}
					</Container>
				}
				<Table celled compact >
					<Table.Header>
						<Table.Row>
							{ !deploy.done &&
								<Table.HeaderCell colSpan="3">
									What will be done
								</Table.HeaderCell>
							}
							{ deploy.done &&
								<Table.HeaderCell colSpan="3">
									<Icon color='green' name='checkmark' />
									Executed commands
								</Table.HeaderCell>
							}
						</Table.Row>
					</Table.Header>
					<Table.Body>
						{deploy.commands.filter(c => _.intersection(c.actions, acts).length > 0).map(::this.renderCommand)}
					</Table.Body>
				</Table>
				{ !deploy.done && (deploy.errors.length === 0) && (analysis.errors.length === 0) &&
						<Button floated='right' color='orange' onClick={::this.deploy}>Go!</Button>
				}
			</Container>
		)
	}

}

const empty = { name: "" }
function mapper(state, props) {
	return {
		analysis: state.analysis.data
		, profile: state.profiles.list[state.currentProfile] || empty
		, branch: state.branches.list[state.currentBranch] || empty
		, project: state.projects.list[state.currentProject] || empty
		, namespace: state.namespaces.list[state.currentNamespace] || empty
	}
}

export default connect(mapper)(Dashboard)
