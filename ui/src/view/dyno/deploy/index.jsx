import React from 'react'
import {
	Label, Form, Input, Accordion, Icon, Feed
	, List, Menu, Card, Container, Grid, Divider
	, Image, Button, Dropdown, Segment, Header, Checkbox
	, Message, Dimmer
} from 'semantic-ui-react'
import { connect } from 'react-redux'

import api from '../../../api'

import Dashboard from './dashboard'
import CommitLog from './commitlog'


class Deploy extends React.Component {
	render() {
		return (
			<Grid padded>
				<Grid.Row centered >
					<Grid.Column width={12}>
						<Dimmer inverted page active={this.props.loading} >
							<Header as='h2' icon >
								<Icon name='snowflake outline' loading />
								Ты-дыщ, ты-дыщ, бум-шмяк, ой-ай..
								<Header.Subheader>Пожалуйста, подождите...</Header.Subheader>
							</Header>
						</Dimmer>
						<Dashboard />
					</Grid.Column>
					<Grid.Column width={4}>
						<CommitLog />
					</Grid.Column>
				</Grid.Row>
			</Grid>
		)
	}
}

function mapper(state, props) {
	return {
		loading: state.analysis.loading
	}
}

export default connect(mapper)(Deploy)
