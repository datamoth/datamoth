import moment from 'moment'
import React from 'react'
import { connect } from 'react-redux'
import * as d3 from 'd3'
import Measure from 'react-measure'
import { Button, Modal } from 'semantic-ui-react'
import Markdown from 'react-markdown'

import api from '../../api'
import help from '../../content/help'

class Help extends React.Component {

	constructor() {
		super(...arguments)
		this.hide = this.hide.bind(this);
	}

	hide() {
		this.props.dispatch(api.hideHelp())
	}

	render() {
		return (
			<Modal open={this.props.help.visible} >
				<Modal.Header>Справка</Modal.Header>
				<Modal.Content>
					<Modal.Description>
						<Markdown source={help} />
					</Modal.Description>
				</Modal.Content>
				<Modal.Actions>
					<Button primary onClick={this.hide} >
						Закрыть
					</Button>
				</Modal.Actions>
			</Modal>
		)
	}

}

function mapStateToProps(state) {
	return {
		help: state.help || {}
	}
}

export default connect(mapStateToProps)(Help)
