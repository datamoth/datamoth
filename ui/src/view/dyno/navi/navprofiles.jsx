import React from 'react'
import { List, Dropdown } from 'semantic-ui-react'
import { connect } from 'react-redux'

import api from '../../../api'

class NavProfiles extends React.Component {

	activate(idx) {
		this.props.dispatch(api.openProfile(idx))
	}

	renderItem(active, i, idx) {
		const isActive = (i.name.endsWith(active))
		const ref = i.name.split("/")
		return (
			<Dropdown.Item key={idx} active={isActive} disabled={isActive} onClick={this.activate.bind(this, idx)} >
				{isActive && <List.Icon name='checkmark' size='small' verticalAlign='middle' />}
				{i.name}
			</Dropdown.Item>
		)
	}

	render() {
		const active = this.props.active.name || ""
		const list = this.props.profiles.list
		return (
			<Dropdown item text={active} >
				<Dropdown.Menu>
					{list.map(this.renderItem.bind(this, active))}
				</Dropdown.Menu>
			</Dropdown>
		)
	}
}

function mapStateToProps(state, props) {
	const empty = { name: '' }

	return {
		profiles: state.profiles
		, active: state.profiles.list[state.currentProfile] || empty
	}
}

export default connect(mapStateToProps)(NavProfiles)
