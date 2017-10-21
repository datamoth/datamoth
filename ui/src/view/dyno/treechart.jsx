import React from 'react'
import * as d3 from 'd3'
import { flatten, keyBy, concat, uniqBy, values, includes, differenceBy } from 'lodash'

class TreeChart extends React.Component {

	constructor() {
		super(...arguments)
		this.state = {
			nmap: {}
			, links: []
			, nodes: []
			, activeNodes: []
			, activeLinks: []
		}
		this.renderLink = this.renderLink.bind(this)
		this.renderActiveNode = this.renderActiveNode.bind(this)
		this.renderActiveLink = this.renderActiveLink.bind(this)
		this.renderNode = this.renderNode.bind(this)
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

		const ilist = flatten(
			coordinators.map( (c) => {
				return c.inputs.map(i => ({ name: c.name, parent: i.name }))
			})
		)
		const olist = flatten(
			coordinators.map( (c) => {
				return c.outputs.map(i => ({ name: i.name, parent: c.name }))
			})
		)

		const omap = keyBy(olist, (i) => i.name)
		const mlist = ilist.filter(i => !omap.hasOwnProperty(i.parent)).map(i => ({ name: i.parent, parent: "root" }))

		const all = concat(ilist, olist, mlist, [{ name: "root", parent: null }])
		const edges = uniqBy(all, i => i.name)

		const extra = differenceBy(all, edges, (d) => d.name + d.parent)

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

		const nmap = keyBy(nodes, n => n.data.name)
		const extraLinks = extra.map(l => {
			const source = nmap[l.parent]
			const target = nmap[l.name]
			return {
				source: source
				, target: target
			}
		})

		return [ nodes, concat(links, extraLinks), nmap ]
	}

	activate(id, uplink, e) {

		uplink = true

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

		const activeNodes = values(nmap).filter(n => n.downlinkActive || n.uplinkActive)
		const u = links.filter(l => includes(activeNodes.filter(n => n.downlinkActive).map(n => n.data.name), l.source.data.name))
		const d = links.filter(l => includes(activeNodes.filter(n => n.uplinkActive).map(n => n.data.name), l.target.data.name))

		this.setState({
			nmap: nmap
			, activeNodes: activeNodes
			, activeLinks: concat(u, d)
		})
	}

	render() {
		const { nodes, links, activeNodes, activeLinks } = this.state
		const glowColor = this.props.nodeGlow
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
					{links.map(this.renderLink)}
					{activeNodes.map(this.renderActiveNode)}
					{activeLinks.map(this.renderActiveLink)}
					{nodes.map(this.renderNode)}
				</g>
			</svg>
		)
	}

	renderLink(l) {
		const { linkColor } = this.props
		const style = {
			fill: "none"
			, stroke: linkColor
			, strokeWidth: "1px"
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
		const activeLinkColor = this.props.activeLinkColor
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
        const {
            nodeRadius
            , textSize
            , nodeColor
            , nodeTextColor
        } = this.props

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
			<g
				key={n.id}
				className="node"
				transform={`translate(${n.x}, ${n.y})`}
				onClick={this.activate.bind(this, n.id, true)}
				onContextMenu={this.activate.bind(this, n.id, false)}
				>
				<foreignObject y="14" x="-24" style={tstyle}>
					<p>{n.id}</p>
				</foreignObject>
				<circle r={nodeRadius} style={nstyle} filter="url(#glow)"/>
			</g>
		)
	}

	renderActiveNode(n) {
		const radius = this.props.activeNodeRadius
		const textSize = this.props.textSize
		const activeNodeColor = this.props.activeNodeColor
		const activeNodeTextColor = this.props.activeNodeTextColor
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
			<g
				key={n.id + n.downlinkActive + n.uplinkActive}
				className="node"
				transform={`translate(${n.x}, ${n.y})`}
				onClick={this.activate.bind(this, n.id, true)}
				onContextMenu={this.activate.bind(this, n.id, false)}
				>
				<foreignObject y="14" x="-24" style={tstyle}>
					<p>{n.id}</p>
				</foreignObject>
				<circle r={radius} style={nstyle} filter="url(#glow)"/>
			</g>
		)
	}
}

TreeChart.defaultProps = {
    activeNodeRadius: 6,
    textSize: '12px',
    activeNodeColor: '#eee',
    activeNodeTextColor: '#ccc',
    nodeGlow: '#aaa',
    linkColor: '#eee',
    nodeRadius: 6,
    nodeColor: '#eee',
    nodeTextColor: '#ccc'
}

export default TreeChart;
