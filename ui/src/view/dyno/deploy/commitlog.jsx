import React from 'react'
import {
	Label, Form, Input, Accordion, Icon, Feed
	, List, Menu, Card, Container, Grid, Link
	, Image, Button, Dropdown, Segment, Header, Checkbox
	, Message, Divider
} from 'semantic-ui-react'
import moment from 'moment'
import { connect } from 'react-redux'

import { EmojiText } from '../../nice/text'
import project from '../../../api'


const imap = {
	'pikachu': 'qq',
	'datamot': 'qq'
}

class CommitLog extends React.Component {

	renderItem(commit) {
		const date = moment(commit.commitDateTime).fromNow()
		const icon = imap[commit.committer] || 'linux'
		return (
			<Feed.Event key={commit.id}>
				<Feed.Label icon={icon} />
				<Feed.Content>
					<Feed.Summary>
						<Feed.User>{commit.committer}</Feed.User>
						<Feed.Date>{date}</Feed.Date>
						<Feed.Date>{commit.commitDateTime}</Feed.Date>
					</Feed.Summary>
					<Feed.Extra text >
						<EmojiText text={commit.msg} />
					</Feed.Extra>
					<Feed.Meta>
						<Feed.Like>
							<Icon name='like' />
							{Math.round(Math.random() * 1000) / 100} Likes
						</Feed.Like>
					</Feed.Meta>
				</Feed.Content>
			</Feed.Event>
		)
	}

	render() {
		const list = this.props.commits.list
		return (
			<div>
				<Divider horizontal >Log</Divider>
				<Feed>
					{list.map(::this.renderItem)}
				</Feed>
			</div>
		)
	}
}

function mapper(state, props) {
	return {
		commits: state.commits
	}
}

export default connect(mapper)(CommitLog)
