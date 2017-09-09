import React from 'react'
import {
	Label, Form, Input, Accordion, Icon, Feed
	, List, Menu, Card, Container, Grid, Link
	, Image, Button, Dropdown, Segment, Header, Checkbox
	, Message, Modal, Dimmer, Divider
} from 'semantic-ui-react'

import { connect } from 'react-redux'
import api from '../../../api'


class Login extends React.Component {

	constructor(props) {
		super(props)
		this.state = { name: "" }
	}

	authenticate() {
		this.props.dispatch(api.enter(this.state.name))
	}

	handleChange(ev) {
		this.setState({name: ev.target.value})
	}

	render() {
		const a = this.props.auth
		return (
			<Dimmer active={!a.authenticated} page >
				<Grid centered >
					<Grid.Column width={4} mobile={4} tablet={3} computer={3} widescreen={2} >
						<Segment inverted>
							<Input value={this.state.name} onChange={::this.handleChange} fluid size='medium' icon='user' placeholder='username' />
							<Divider horizontal inverted >:-:</Divider>
							{this.state.name.length > 0 &&
								<Button fluid size='mini' onClick={::this.authenticate}>go</Button>
							}
							{a.failed && <Message color='purple' >{a.errormsg}</Message>}
						</Segment>
					</Grid.Column>
				</Grid>
			</Dimmer>
		)
	}
}

function mapper(state, props) {
	return { auth: state.auth }
}

export default connect(mapper)(Login)
