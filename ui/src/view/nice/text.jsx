import React from 'react'
import PropTypes from 'prop-types'

class EmojiText extends React.Component {

	insertEmoji(txt) {
		return txt.replace(/:([a-zA-Z_-]+):/g, "<i class='em em-$1'></i>")
	}

	render() {
		const txt = this.insertEmoji(this.props.text)
		return (
			<p dangerouslySetInnerHTML={{__html: txt}} >
			</p>
		)
	}
}

EmojiText.propTypes = {
	text: PropTypes.string
}

EmojiText.defaultProps = {
	text: ''
}

export { EmojiText }
