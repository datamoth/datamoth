import React from 'react'
import { List, Dropdown } from 'semantic-ui-react'
import { connect } from 'react-redux'

import api from '../../../api'

function getref(path) {
	const refpath = path.split("/")
	const ref = refpath[refpath.length - 1]
	return ref
}

class NavBranches extends React.Component {

	activate(idx) {
		this.props.dispatch(api.openBranch(idx))
	}

	renderItem(active, i, idx) {
		const isActive = (i.name.endsWith(active))
		const ref = getref(i.name)
		return (
			<Dropdown.Item key={idx} active={isActive} disabled={isActive} onClick={this.activate.bind(this, idx)} >
				{isActive && <List.Icon name='checkmark' size='small' verticalAlign='middle' />}
				{ref}
			</Dropdown.Item>
		)
	}

	render() {
		const active = this.props.active.name || ""
		const list = this.props.branches.list
		return (
			<Dropdown item text={getref(active) + " "}>
				<Dropdown.Menu>
					{list.map(this.renderItem.bind(this, active))}
				</Dropdown.Menu>
			</Dropdown>
		)
	}
}

function mapStateToProps(state, props) {
	const empty = { name: "" }
	return {
		branches: state.branches
		, active: state.branches.list[state.currentBranch] || empty
	}
}

export default connect(mapStateToProps)(NavBranches)
