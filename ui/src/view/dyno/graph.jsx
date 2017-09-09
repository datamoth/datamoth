import _ from 'lodash'
import moment from 'moment'
import React from 'react'
import { connect } from 'react-redux'
import * as d3 from 'd3'
import Measure from 'react-measure'
import toposort from 'toposort'
import {
	Label, Form, Input, Accordion, Icon, Feed
	, List, Menu, Card, Container, Grid, Link
	, Image, Button, Dropdown, Segment, Header, Checkbox
	, Message, Dimmer
} from 'semantic-ui-react'

import api from '../../api'


class TreeChart extends React.Component {

	constructor(props, ctx) {
		super(props, ctx)
		this.state = {
			nmap: {}
			, links: []
			, nodes: []
			, activeNodes: []
			, activeLinks: []
		}
	}

	componentDidMount() {
		const [ nodes, links, nmap ] = this.layout()
		this.setState({
			nmap: nmap
			, links: links
			, nodes: nodes
			, activeNodes: []
			, activeLinks: []
		})
	}

	walk() {
		const graph = this.props.graph
		const coordinators = graph.coordinators || []

		const ilist = _.flatten(
			coordinators.map( (c) => {
				return c.inputs.map(i => ({ name: c.name, parent: i.name }))
			})
		)
		const olist = _.flatten(
			coordinators.map( (c) => {
				return c.outputs.map(i => ({ name: i.name, parent: c.name }))
			})
		)

		const omap = _.keyBy(olist, (i) => i.name)
		const mlist = ilist.filter(i => !omap.hasOwnProperty(i.parent)).map(i => ({ name: i.parent, parent: "root" }))

		const all = _.concat(ilist, olist, mlist, [{ name: "root", parent: null }])
		const edges = _.uniqBy(all, i => i.name)

		const extra = _.differenceBy(all, edges, (d) => d.name + d.parent)

		return [ edges, extra ]
	}

	layout() {
		const w = this.props.width - 30
		const h = this.props.height - 120

		const [ edges, extra ] = this.walk()

		const treeData = d3
			.stratify()
			.id((d) => d.name)
			.parentId((d) => d.parent)
			(edges)

		const tree = d3
			.tree()
			.size([ w, h ])
			(treeData)

		const nodes = tree.descendants()
		const links = tree.links()

		const nmap = _.keyBy(nodes, n => n.data.name)
		const extraLinks = extra.map(l => {
			const source = nmap[l.parent]
			const target = nmap[l.name]
			return {
				source: source
				, target: target
			}
		})

		return [ nodes, _.concat(links, extraLinks), nmap ]
	}

	activate(id, uplink, e) {

		var uplink = true

		if (e.nativeEvent.which === 3) {
			uplink = false
			e.preventDefault()
		}

		const { nmap, links } = this.state

		if (nmap[id].uplinkActive && uplink) {
			nmap[id].uplinkActive = false
		} else if (nmap[id].downlinkActive && !uplink) {
			nmap[id].downlinkActive = false
		} else if (!nmap[id].uplinkActive && uplink) {
			nmap[id].uplinkActive = true
		} else if (!nmap[id].downlinkActive && !uplink) {
			nmap[id].downlinkActive = true
		}

		const activeNodes = _.values(nmap).filter(n => n.downlinkActive || n.uplinkActive)
		const u = links.filter(l => _.includes(activeNodes.filter(n => n.downlinkActive).map(n => n.data.name), l.source.data.name))
		const d = links.filter(l => _.includes(activeNodes.filter(n => n.uplinkActive).map(n => n.data.name), l.target.data.name))

		this.setState({
			nmap: nmap
			, activeNodes: activeNodes
			, activeLinks: _.concat(u, d)
		})
	}

	render() {
		const { nodes, links, activeNodes, activeLinks } = this.state
		const glowColor = this.props.nodeGlow || "#aaaaaa"
		return (
			<svg width="100%" height="100%">
				<defs>
					<filter id="byaka">
						<feTurbulence type="turbulence" baseFrequency="0.05" numOctaves="3" result="turbulence"/>
						<feDisplacementMap in2="turbulence" in="SourceGraphic" scale="8" xChannelSelector="R" yChannelSelector="G"/>
					</filter>
					<filter id="glow" height="350%" width="350%" x="-50%" y="-50%">
						<feMorphology operator="dilate" radius="1" in="SourceAlpha" result="thicken" />
						<feGaussianBlur in="thicken" stdDeviation="2" result="blurred" />
						<feFlood floodColor={glowColor} result="glowColor" />
						<feComposite in="glowColor" in2="blurred" operator="in" result="softGlow_colored" />
						<feMerge>
							<feMergeNode in="softGlow_colored"/>
							<feMergeNode in="SourceGraphic"/>
						</feMerge>
					</filter>
				</defs>
				<g transform="translate(0, 30)">
					{links.map(::this.renderLink)}
					{activeNodes.map(::this.renderActiveNode)}
					{activeLinks.map(::this.renderActiveLink)}
					{nodes.map(::this.renderNode)}
				</g>
			</svg>
		)
	}

	renderLink(l) {
		const linkColor = this.props.linkColor || "#333333"
		const style = {
			fill			: "none"
			, stroke		: linkColor
			, strokeWidth	: "1px"
		}
		function path(d) {
			return "M" + d.source.x + "," + d.source.y
				+ "C" + (d.source.x + d.target.x) / 2 + "," + d.source.y
				+ " " + (d.source.x + d.target.x) / 2 + "," + d.target.y
				+ " " + d.target.x + "," + d.target.y;
		}
		return (
			<path key={`${l.source.id}-${l.target.id}`} style={style} d={path(l)} />
		)
	}

	renderActiveLink(l) {
		const activeLinkColor = this.props.activeLinkColor || "#eeeeee"
		const style = {
			fill			: "none"
			, stroke		: activeLinkColor
			, strokeWidth	: "1px"
		}

		function path(d) {
			return "M" + d.source.x + "," + d.source.y
				+ "C" + (d.source.x + d.target.x) / 2 + "," + d.source.y
				+ " " + (d.source.x + d.target.x) / 2 + "," + d.target.y
				+ " " + d.target.x + "," + d.target.y;
		}

		return (
			<path key={`${l.source.id}-${l.target.id}`} style={style} d={path(l)} />
		)
	}

	renderNode(n) {
		const radius = this.props.nodeRadius || 6
		const textSize = this.props.textSize || "12px"
		const nodeColor = this.props.nodeColor || "#eeeeee"
		const nodeTextColor = this.props.nodeTextColor || "#cccccc"
		const nstyle = {
			fill: nodeColor
			, stroke: nodeColor
			, strokeWidth: "1px"
		}
		const tstyle = {
			fontSize: textSize
			, width: 50
			, textAlign: "center"
			, color: nodeTextColor
		}
		return (
			<g key={n.id} className="node" transform={`translate(${n.x}, ${n.y})`}

						onClick={this.activate.bind(this, n.id, true)}
						onContextMenu={this.activate.bind(this, n.id, false)} >

				<foreignObject y="14" x="-24" style={tstyle}>
					<p>{n.id}</p>
				</foreignObject>
				<circle r={radius} style={nstyle} filter="url(#glow)"/>
			</g>
		)
	}

	renderActiveNode(n) {
		const radius = this.props.activeNodeRadius || 6
		const textSize = this.props.textSize || "12px"
		const activeNodeColor = this.props.activeNodeColor || "#eeeeee"
		const activeNodeTextColor = this.props.activeNodeTextColor || "#cccccc"
		const nstyle = {
			fill: activeNodeColor
			, stroke: activeNodeColor
			, strokeWidth: "1px"
		}
		const tstyle = {
			fontSize: textSize
			, width: 50
			, textAlign: "center"
			, color: activeNodeTextColor
		}
		return (
			<g key={n.id + n.downlinkActive + n.uplinkActive} className="node" transform={`translate(${n.x}, ${n.y})`}

						onClick={this.activate.bind(this, n.id, true)}
						onContextMenu={this.activate.bind(this, n.id, false)} >

				<foreignObject y="14" x="-24" style={tstyle}>
					<p>{n.id}</p>
				</foreignObject>
				<circle r={radius} style={nstyle} filter="url(#glow)"/>
			</g>
		)
	}
}

class Graph extends React.Component {

	constructor(props) {
		super(props)
		this.state = {
			dims: { width: 1, height: 1 }
			, snowflakes: []
		}
	}

	componentDidMount() {
		this.setState({
			timer: setInterval(::this.stepSnow, 50)
		})
	}

	componentWillUnmount() {
		clearTimeout(this.state.timer)
		this.setState({
			timer: null
		})
	}

	setDims(dims) {
		this.setState({ dims })
	}

	stopSnow() {
		this.setState({
			snowflakes: []
		})
	}

	hide() {
		this.stopSnow()
		this.props.dispatch(api.hideGraph())
	}

	stepSnow() {
		const self = this
		const snowflakes = this.state.snowflakes.map(s => {
			if (s.y > 1000) {
				return self.createSnowflake()
			}
			return ({ x: s.x, y: s.y + s.v, v: s.v, k: s.k, s: s.s })
		})
		if (snowflakes.length > 0) {
			this.setState({
				snowflakes: snowflakes
			})
		}
	}

	addSnowflake() {
		const snowflake = this.createSnowflake()
		this.setState({
			snowflakes: _.concat(this.state.snowflakes, [ snowflake ])
		})
	}

	createSnowflake() {
		const size = [
			'mini',
			'mini',
			'mini',
			'mini',
			'tiny',
			'small',
			'small',
			'massive',
			'mini',
			'small',
			'small',
			'mini',
			'small',
			'small',
			'mini',
			'large',
			'big',
			'small',
			'mini',
			'huge',
			'mini',
			'small'
		]
		return {
			x: Math.round(Math.random() * 2000)
			, y: 0
			, v: Math.round(Math.random() * 10) % 3 + 1
			, k: Math.round(Math.random() * 10000)
			, s: size[Math.round(Math.random() * 100) % (size.length - 1)]
		}
	}

	renderSnowflake(s) {
		return (
			<Icon key={s.k} size={s.s} name="snowflake outline" style={{
				position: "absolute"
				, left: s.x
				, top: s.y
			}} />
		)
	}

	render() {
		const color = '#3f5175'
		const style = {
			height: "1000px"
		}
		return (
			<Dimmer page active={this.props.graph.visible}>
				{this.state.snowflakes.map(::this.renderSnowflake)}
				<Icon name="remove" size="large" onClick={::this.hide} style={{ position: "absolute", top: 5, right: 5 }} />
				<div style={{ position: "absolute", bottom: 15, left: 10, fontSize: "9px", color: "grey" }}>
					<small>Made with </small><Icon name="heart" size="small" onClick={::this.addSnowflake}/><small>by</small> <Icon name="child" size="large" onClick={::this.stopSnow} />
				</div>
				<Measure onMeasure={::this.setDims}>
					<Container fluid style={style} >
						<TreeChart
							key={Math.random()}
							graph={this.props.analysis}
							width={this.state.dims.width}
							height={this.state.dims.height}
							linkColor="#333333"
							activeLinkColor="#cccccc"
							nodeColor="#cccccc"
							activeNodeColor="#eeeeee"
							nodeGlow="#aaaaaa"
							nodeRadius={5}
							activeNodeRadius={5}
							nodeTextColor="#aaaaaa"
							activeNodeTextColor="white"
							textSize="12px"
						/>
					</Container>
				</Measure>
			</Dimmer>
		)
	}
}

const empty = { name: "" }
function mapper(state, props) {
	return {
		graph: state.graph
		, analysis: state.analysis.data
		, profile: state.profiles.list[state.currentProfile] || empty
		, branch: state.branches.list[state.currentBranch] || empty
		, project: state.projects.list[state.currentProject] || empty
		, namespace: state.namespaces.list[state.currentNamespace] || empty
	}
}

export default connect(mapper)(Graph)
