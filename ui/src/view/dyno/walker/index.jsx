import React from 'react'
import {
	Label, Table, Icon, Divider
	, Container, Grid 
	, Button, Segment, Header
	, Menu
} from 'semantic-ui-react'
import { connect } from 'react-redux'
import Select from 'react-select'

import 'react-select/dist/react-select.css'
import moment from 'moment'

import api from '../../../api'
import Editor from './editor'

import 'brace/mode/xml'
import 'brace/theme/github'

function formatBytes(bytes, decimals) {
	if (bytes === 0)
		return '0 Bytes';
	const k = 1000,
		dm = decimals || 2,
		sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'],
		i = Math.floor(Math.log(bytes) / Math.log(k));
	return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

function statusIcon(status, size) {
	const sz = size || "mini"
	switch (status) {
	case "FAILED"		: return <Label size={sz} color="red">{status}</Label>
	case "IGNORED"		: return <Label size={sz} color="red">{status}</Label>
	case "KILLED"		: return <Label size={sz} color="red">{status}</Label>
	case "READY"		: return <Label size={sz} color="orange">{status}</Label>
	case "RUNNING"		: return <Label size={sz} color="orange">{status}</Label>
	case "SKIPPED"		: return <Label size={sz} color="red">{status}</Label>
	case "SUBMITTED"	: return <Label size={sz} color="orange">{status}</Label>
	case "SUCCEEDED"	: return <Label size={sz} color="green">{status}</Label>
	case "SUSPENDED"	: return <Label size={sz} color="orange">{status}</Label>
	case "TIMEDOUT"		: return <Label size={sz} color="red">{status}</Label>
	case "WAITING"		: return <Label size={sz} color="orange">{status}</Label>
	default				: return <Label size={sz} color="orange">UNKNOWN</Label>
	}
}

function dspath(path) {
	return path.split("/").filter(p => p.indexOf("$") === -1).join("/").trim()
}

function dshuepath(hue, path) {
	return hue + dspath(path)
}

class Dir extends React.Component {
	render() {
		return (
			<Divider horizontal >
				<Icon name="chevron down" />
			</Divider>
		)
	}
}

class Walker extends React.Component {

	constructor(props) {
		super(props)
		this.state = {
			cidx: 0
			, item: "c"
			, name: ""
			, file: ""
			, inputs: []
			, outputs: []
		}
	}

	findUpstream(ds, data) {
		const cc = []
		for (var i = 0; i < data.coordinators.length; ++i) {
			const c = data.coordinators[i]
			var has = false
			for (var j = 0; j < c.outputs.length && !has; ++j) {
				if (c.outputs[j].dataset && c.outputs[j].dataset.name === ds.name && c.outputs[j].dataset.location.file === ds.location.file) {
					has = true
				}
			}
			if (has) {
				cc.push(c)
			}
		}
		return cc
	}

	findDownstream(ds, data) {
		const cc = []
		for (var i = 0; i < data.coordinators.length; ++i) {
			const c = data.coordinators[i]
			var has = false
			for (var j = 0; j < c.inputs.length && !has; ++j) {
				if (c.inputs[j] && c.inputs[j].dataset.name === ds.name && c.inputs[j].dataset.location.file === ds.location.file) {
					has = true
				}
			}
			if (has) {
				cc.push(c)
			}
		}
		return cc
	}

	chooseDataset(d, data) {
		const ns = this.props.activeNamespace.name
		const pr = this.props.activeProject.name
		const rf = this.props.activeRef.name
		const pf = this.props.activeProfile.name
		const path = d.uri.split("/").filter(p => p.indexOf("$") === -1).join("/").trim()
		this.props.dispatch(api.hdfsLs(ns, pr, rf, pf, path))
		this.setState({
			item: "d"
			, name: d.name
			, file: d.location.file
			, inputs: this.findUpstream(d, data)
			, outputs: this.findDownstream(d, data)
			, uri: d.uri
		})
	}

	chooseCoordinator(cname, data) {
		var idx = 0
		for (idx = 0; idx < data.coordinators.length; ++idx) {
			if (data.coordinators[idx].name === cname) {
				break;
			}
		}
		const c = data.coordinators[idx]

		const ns = this.props.activeNamespace.name
		const pr = this.props.activeProject.name
		const rf = this.props.activeRef.name
		const pf = this.props.activeProfile.name

		const wfiles = c.workflow.files.filter(f => !f.file.startsWith("/")).map(f => f.file)

		this.props.dispatch(api.getFiles(ns, pr, rf, pf
			, [ c.location.file, c.workflow.location.file ].concat(wfiles)
		))
		this.props.dispatch(api.getOozieCoordinatorInfo(ns, pr, rf, pf, cname))

		this.setState({
			item: "c"
			, cidx: idx
			, name: cname
		})
	}

	prevC(data) {
		var cidx = this.state.cidx
		if (cidx === 0) {
			cidx = data.coordinators.length - 1
		} else {
			cidx -= 1
		}
		this.setState({ cidx: cidx })
	}

	nextC(data) {
		var cidx = this.state.cidx
		if (cidx === data.coordinators.length - 1) {
			cidx = 0
		} else {
			cidx += 1
		}
		this.setState({ cidx: cidx })
	}

	selectCoordinator(data, opt) {
		this.chooseCoordinator(opt.value, data)
	}

	selectDataset(data, opt) {
		const name = opt.label.split(":")[0]
		const file = opt.label.split(":")[1]
		const dlist = _.flatMap(data.databundles.map(b => b.datasets.map(d => d)))
		for (var i = 0; i < dlist.length; ++i) {
			if (dlist[i].name === name && dlist[i].location.file === file) {
				this.chooseDataset(dlist[i], data)
				return
			}
		}
	}

	renderC(data, editor, cc) {
		const c = data.coordinators[this.state.cidx]
		const clist = data.coordinators.map(c => ({ value: c.name, label: c.name }))
		const ccLastActionTime = moment(cc.lastActionTime).fromNow()
		const ccNextActionTime = moment(cc.nextMaterializedTime).fromNow()
		return (
			<Container fluid >
				{ c && 
					<Container fluid >
						<Grid padded stackable >
							<Grid.Column width={4}>
								<Menu vertical fluid inverted color="blue" >
									<Menu.Item>{ cc.waiting && <Icon name="spinner" loading /> }Coordinator</Menu.Item>
									<Menu.Item>
										<Select
											name="coordinator-chooser"
											value={this.state.name}
											options={clist}
											onChange={this.selectCoordinator.bind(this, data)}
										/>
									</Menu.Item>
									<Menu.Item><Label size='large' color='grey' >{c.start}</Label>Start</Menu.Item>
									<Menu.Item><Label size='large' color='grey' >{c.end}</Label>End</Menu.Item>
									<Menu.Item><Label size='large' color='grey' >{c.timeout}</Label>Timeout</Menu.Item>
									{ cc.exists &&
										<div>
											<Menu.Item><Label size='large' color='grey' >{cc.id}</Label>ID</Menu.Item>
											<Menu.Item><Label size='large' color='grey' >{cc.user}</Label>User</Menu.Item>
											<Menu.Item>{statusIcon(cc.status, "large")}Status</Menu.Item>
											<Menu.Item><Label size='large' color='grey' >{ccLastActionTime}</Label>Last time</Menu.Item>
											<Menu.Item><Label size='large' color='grey' >{ccNextActionTime}</Label>Next time</Menu.Item>
										</div>
									}
								</Menu>
								{ cc.exists && !cc.waiting &&
								<Segment textAlign="center" >
									<Label attached='top' >Run Log</Label>
									<Table basic="very" celled collapsing compact stackable >
										<Table.Body>
										{ cc.actions.map((a, i) => (
											<Table.Row key={i}>
												<Table.Cell>{a.actionNumber}</Table.Cell>
												<Table.Cell>{statusIcon(a.status)}</Table.Cell>
												<Table.Cell>{moment(a.createdTime).format('YYYY-MM-DD')}</Table.Cell>
												<Table.Cell>{moment(a.createdTime).format('hh:mm')}</Table.Cell>
												<Table.Cell>{moment(a.createdTime).fromNow()}</Table.Cell>
												<Table.Cell>
													{a.externalId &&
														<a target="_blank" href={data.hueUrl + "/oozie/list_oozie_workflow/"+a.externalId+"/"}>
														W
													</a>
													}
													{!a.externalId &&
														<Icon name="time" />
													}
												</Table.Cell>
											</Table.Row>
										))}
										</Table.Body>
									</Table>
								</Segment>
								}
							</Grid.Column>
							<Grid.Column width={12}>

								<Header as='h3' textAlign='left' color='blue'>Cooridnator {c.name}</Header>
								<Divider hidden />
								
								{c.inputs.length > 0 &&
									<Container fluid >
										<Header as='h3' textAlign='left' color='blue'>Depends on</Header>
										<Grid columns={5} stackable={true}>
											{c.inputs.map((ds, idx) => (
												<Grid.Column key={idx}>
													<Button basic fluid onClick={this.chooseDataset.bind(this, ds.dataset, data)}>{ds.datasetName}</Button>
												</Grid.Column>
											))}
										</Grid>
									</Container>
								}

								<Divider hidden />

								{c.outputs.length > 0 &&
									<Container fluid >
										<Header as='h3' textAlign='left' color='blue'>Produces</Header>
										<Grid columns={5} >
											{c.outputs.map((ds, idx) => (
												<Grid.Column key={idx} >
													<Button basic fluid onClick={this.chooseDataset.bind(this, ds.dataset, data)}>{ds.datasetName}</Button>
												</Grid.Column>
											))}
										</Grid>
									</Container>
								}

								<Header as='h3' textAlign='left' color='blue'>Related files</Header>
								<Editor />
							</Grid.Column>
						</Grid>
					</Container>
				}
			</Container>
		)
	}

	renderD(data, dd) {
		const c = data.coordinators[this.state.cidx]
		const dlist = _.flatMap(data.databundles.map(b => b.datasets.map(d => ({ value: d.name, label: d.name + ":" + d.location.file }))))
		return (
			<Container fluid >
				<Grid padded stackable >
					<Grid.Column width={4}>
						<Menu vertical fluid inverted color="blue" >
							<Menu.Item>{ dd.waiting && <Icon name="spinner" loading /> }Dataset</Menu.Item>
							<Menu.Item>
								<Select
									name="dataset-chooser"
									value={this.state.name}
									options={dlist}
									onChange={this.selectDataset.bind(this, data)}
								/>
							</Menu.Item>
							<Menu.Item><Label size='large' color='grey' >{this.state.name}</Label>Name</Menu.Item>
							<Menu.Item><Label size='large' color='grey' >{this.state.file}</Label>Bundle</Menu.Item>
							{ dd.size !== -1 && <Menu.Item><Label size='large' color='grey' >{formatBytes(dd.size)}</Label>Size</Menu.Item> }
							{ dd.space !== -1 && <Menu.Item><Label size='large' color='grey' >{formatBytes(dd.space)}</Label>Space</Menu.Item> }
						</Menu>
						{ dd.exists && !dd.waiting &&
						<Segment textAlign="center" >
							<Label attached='top' >Content</Label>
							<Table basic="very" celled compact stackable >
								<Table.Header>
									<Table.Row>
										<Table.HeaderCell>Type</Table.HeaderCell>
										<Table.HeaderCell>Modified</Table.HeaderCell>
										<Table.HeaderCell>Size</Table.HeaderCell>
										<Table.HeaderCell>Space</Table.HeaderCell>
										<Table.HeaderCell>Name</Table.HeaderCell>
									</Table.Row>
								</Table.Header>
								<Table.Body>
								{ dd.items.map((item, i) => (
									<Table.Row key={i}>
										<Table.Cell>
											{ item.isDir && <Icon name="folder" /> }
											{ !item.isDir && <Icon name="file" /> }
										</Table.Cell>
										<Table.Cell>{moment(item.modified).fromNow()}</Table.Cell>
										<Table.Cell>{formatBytes(item.size)}</Table.Cell>
										<Table.Cell>{formatBytes(item.space)}</Table.Cell>
										<Table.Cell>
											<a target="_blank" href={dshuepath(data.hueUrl + "/filebrowser/#", item.path)}>{item.name}</a>
										</Table.Cell>
									</Table.Row>
								))}
								</Table.Body>
							</Table>
						</Segment>
						}
					</Grid.Column>
					<Grid.Column width={12}>
						<Header as='h3' textAlign='left' color='blue'>Dataset {this.state.name}</Header>
						<a target="_blank" href={dshuepath(data.hueUrl + "/filebrowser/#", this.state.uri)}>{this.state.uri}</a>
						<Divider hidden />
						{this.state.inputs.length > 0 &&
							<Container fluid >
								<Header as='h3' textAlign='left' color='blue'>Produced by</Header>
								<Grid columns={3} stackable={true}>
									{this.state.inputs.map((c, idx) => (
										<Grid.Column key={idx}>
											<Button basic fluid onClick={this.chooseCoordinator.bind(this, c.name, data)}>{c.name}</Button>
										</Grid.Column>
									))}
								</Grid>
							</Container>
						}
						<Divider hidden />
						{this.state.outputs.length > 0 &&
							<Container fluid >
								<Header as='h3' textAlign='left' color='blue'>Used by</Header>
								<Grid columns={3} stackable={true}>
									{this.state.outputs.map((c, idx) => (
										<Grid.Column key={idx} >
											<Button basic fluid onClick={this.chooseCoordinator.bind(this, c.name, data)}>{c.name}</Button>
										</Grid.Column>
									))}
								</Grid>
							</Container>
						}
					</Grid.Column>
				</Grid>
			</Container>
		)
	}

	renderTable(data) {
		const list = data.coordinators
		return (
			<Table celled striped >
				<Table.Body>
				{ list.map(c => (
					<Table.Row>
						<Table.Cell>{c.name}</Table.Cell>
					</Table.Row>
				)) }
				</Table.Body>
			</Table>
		)
	}

	render() {
		const data = this.props.analysis
		const editor = this.props.editor
		const currentDataset = this.props.currentDataset
		const currentCoordinator = this.props.currentCoordinator
		return (
			<Container fluid style={{ marginTop: "60px" }}>
				{ (this.state.item === "d") && <div>{this.renderD(data, currentDataset)}</div> }
				{ (this.state.item === "c") && <div>{this.renderC(data, editor, currentCoordinator)}</div> }
			</Container>
		)
	}

}

const empty = { name: "" }
function mapper(state, props) {
	return {
		analysis: state.analysis.data
		, editor: state.editor
		, currentCoordinator: state.currentCoordinator
		, currentDataset: state.currentDataset
		, activeNamespace: state.namespaces.list[state.currentNamespace] || empty
		, activeProject: state.projects.list[state.currentProject] || empty
		, activeRef: state.branches.list[state.currentBranch] || empty
		, activeProfile: state.profiles.list[state.currentProfile] || empty
	}
}

export default connect(mapper)(Walker)
