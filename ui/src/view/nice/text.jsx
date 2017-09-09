import React from 'react'



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

export { EmojiText }
