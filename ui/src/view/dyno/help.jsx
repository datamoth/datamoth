import moment from 'moment'
import React from 'react'
import { connect } from 'react-redux'
import * as d3 from 'd3'
import Measure from 'react-measure'
import {
	Label, Form, Input, Accordion, Icon, Feed
	, List, Menu, Card, Container, Grid, Link
	, Image, Button, Dropdown, Segment, Header, Checkbox
	, Message, Dimmer, Modal
} from 'semantic-ui-react'
import Markdown from 'react-markdown'

import api from '../../api'


const help =
`
Все прочитанное может быть использовано, а может и не быть, использовано. Разрешается
читать, критиковать, всячески распространять, агитировать, призывать к активным действиям
на основе, хихикать над, использовать для завоевания какой-нибудь страны и внедрения на ее
территории своей идеологии.

Прежде чем все это делать не забудьте выбрать нужную ветку и нужный профиль.


#### Что такое ветка?

Ветка это обычная ветка git которая запушена в репозиторий Датамота. Все запушенные ветки
будут отображены в списке веток на панели меню.


#### Что такое профиль?

Профиль это набор переменных. В качестве профиля можно использовать любой раздел
настроек. Чтобы лучше ознакомиться с настройками почитайте про HOCON (носон). Чтобы
раздел стал профилем его нужно указать в разделе [profiles] в корневом [.conf]
файле проекта:

\`\`\`
profiles: [
  { name: "emotion.joyful"      , description: "Create and spread joy" }
  , { name: "emotion.painful"   , description: "Born and sow pain" }
]

emotion: {
  joyful: {
	actions: [ "hello", "hi" ]
	jobTracker:	"ew-mmp-nnode1.kcell.kz:8032"
	nameNode:	"ew-mmp-nnode1.kcell.kz:8020"
  }
}
\`\`\`

Для указанной настройки в Датамоте появятся профили: [emotion.joyful, emotion.painful],
к переменным можно будет обращаться так:

\`\`\`
{{actions}}
{{jobTracker}}
\`\`\`

то есть уже без указания пути к профилю.


#### Как добавлять координатор? 

Опишите координатор в любой папке проекта. Чтобы он был обнаружен, расширение
файла содержащего координатор должно быть xml. Имя указанное в атрибуте [name]
будет впоследствии использоваться как ссылка на него.


Чтобы ссылаться на другие файлы проекта, например, чтобы включить dataset-ы в
координатор нужно использовать относительный путь до файла с dataset-ами, при
этом используя префикс {{PROJECT_DIR}}:

\`\`\`
	<datasets>
		<include>{{PROJECT_DIR}}/datasets/joyful.xml</include>
		<include>{{PROJECT_DIR}}/datasets/painful.xml</include>
	</datasets>
\`\`\`

То же относится к ссылке с файлом workflow:

\`\`\`
	<action>
		<workflow>
			<app-path>{{PROJECT_DIR}}/jobs/create-delight/workflow.xml</app-path>
			<configuration>
	...
\`\`\`


#### Как работает накатка?

Плохо.

Для управления координаторами (установка, запуска, остановка и т.д.) применяется
файлик [.deploy.conf], который должен лежать в корне проекта и иметь следующую
структуру:

\`\`\`
{
    "deploy" : {
        "oozie" : {
            "coordinators" : {
                "create-delight" : "kill, start"
                "create-happiness" : "pause"
                "create-sunshine" : "kill"
            }
            "done" : false
            "errors" : []
        }
    }
}
\`\`\`

В разделе [coordinators] перечисляются координаторы и действия которые должны
быть с ними произведены.  Порядок выполнения действий гарантируется только в
рамках одного координатора. Булевый флаг [done] сигнализизирует о том, что
действия еще не произведены, в силу того что Датамот был очень занят.  Если он
установлен в [false], и нет ошибок, в интерфейсе появится оранжевая кнопка [Поехали!],
при клике на которую, координаторы (весь проект) будут скопированы на кластер,
и выполнятся указанные комманды.

После выполнения комманд, Датамот сделает ответный коммит в репозиторий проекта,
в котором будет содержаться информация о произошедшей катастрофе. Если сообщение
в коммите положительное - нет ошибок, и всё прошло успешно. Если есть легкая грустинка,
это означает что были ошибки, и они должны быть уже видны на странице полётов. Коммиты
следует читать снизу вверх, как в git.

Так как был произведён коммит со стороны Датамота, нужно забрать изменения из
удалённого репозитория, для ветки с которой идёт работа.

Важный момент, когда нужно что-то сделать с существующим координатором, где-то
должен лежать его ID. Датамот не сможет ничего сделать с координатором который
был запущен не им (пока ещё). После первичного запуска координатора, его ID
сохраняется в файлике [.state.conf] в корне проекта:

\`\`\`
{
	"state" : {
		"oozie" : {
			"create-delight" : "running|0001475-170310134619804-oozie-oozi-C",
			"create-happiness" : "running|0001473-170310134619804-oozie-oozi-C",
			"create-sunshine" : "running|0001474-170310134619804-oozie-oozi-C"
		}
	}
}
\`\`\`

Коммит в котором лежит последнее состояние помечается тегом [deployed]. Именно из
него Датамот считывает состояние, чтобы иметь возможность выполнять комманды.


#### Как можно менять системные настройки oozie и hdfs:

Системные настройки hdfs, oozie указываются в разделе sysoptions в корневом [.conf] файле
проекта:

\`\`\`
sysoptions: {
	hdfs: {
		fs.defaultFS: "hdfs://ew-mmp-nnode1.kcell.kz:8020"
		hadoop.security.authentication: "simple"
		hadoop.security.authorization: "false"
	}
	oozie: {
		uri: "http://ew-mmp-nnode1.kcell.kz:11000/oozie"
	}
}

\`\`\`


#### Инструкция к установке

В процессе извлечения

`

class Help extends React.Component {

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
					<Button primary onClick={::this.hide} >
						Закрыть
					</Button>
				</Modal.Actions>
			</Modal>
		)
	}

}

function mapper(state, props) {
	return {
		help: state.help
	}
}

export default connect(mapper)(Help)
