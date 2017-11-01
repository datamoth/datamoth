import React from 'react'
import PropTypes from 'prop-types'
import { Container, Menu } from 'semantic-ui-react'
import { connect } from 'react-redux'
import AceEditor from 'react-ace'

import 'react-select/dist/react-select.css'

import 'brace/mode/xml'
import 'brace/mode/python'
import 'brace/theme/textmate'

function mode(file) {
	if (!file) return "text"
	if (file.location.file.endsWith(".py")) return "python"
	if (file.location.file.endsWith(".xml")) return "xml"
	return "text"
}

class Editor extends React.Component {

	constructor(props) {
		super(props)
		this.state = {
			idx: 0
		}
	}

	choose(idx) {
		this.setState({ idx: idx })
	}

	render() {
		const self = this
		const { idx } = this.state
		const files = this.props.editor
		return (
			<Container fluid>
				{ files.length > 0 && 
					<Container fluid>
						<Menu pointing secondary>
							{ files.map((f, i) => (
								<Menu.Item key={i} active={idx === i} onClick={self.choose.bind(self, i)}>
									{f.location.file}
								</Menu.Item>
							))}
						</Menu>
						<AceEditor
							mode={mode(files[idx])}
							width="100%"
							theme="textmate"
							fontSize={16}
							readOnly={true}
							value={files[idx].content}
						/>
					</Container>
				}
			</Container>
		)
	}
}

Editor.propTypes = {
	editor: PropTypes.array
}

Editor.defaultProps = {
	editor: []
}

function mapStateToProps(state) {
	return {
		editor: state.editor
	}
}

export default connect(mapStateToProps)(Editor)
