import React from 'react'
import { connect } from 'react-redux'
import { concat } from 'lodash'
import Measure from 'react-measure'
import { Icon, Container, Dimmer } from 'semantic-ui-react'
import TreeChart from './treechart'

import api from '../../api'

class Graph extends React.Component {

	constructor(props) {
		super(props)
		this.state = {
			dims: { width: 1, height: 1 }
			, snowflakes: []
		}
		this.stepSnow = this.stepSnow.bind(this)
	}

	componentDidMount() {
		this.setState({
			timer: setInterval(this.stepSnow, 50)
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
			snowflakes: concat(this.state.snowflakes, [ snowflake ])
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

function mapStateToProps(state, props) {
	const empty = { name: "" }

	return {
		graph: state.graph
		, analysis: state.analysis.data
		, profile: state.profiles.list[state.currentProfile] || empty
		, branch: state.branches.list[state.currentBranch] || empty
		, project: state.projects.list[state.currentProject] || empty
		, namespace: state.namespaces.list[state.currentNamespace] || empty
	}
}

export default connect(mapStateToProps)(Graph)
