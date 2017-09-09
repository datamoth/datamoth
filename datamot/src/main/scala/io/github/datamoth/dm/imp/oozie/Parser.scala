package io.github.datamoth.dm.imp.oozie

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File
import java.nio.file.Files
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils

import io.github.datamoth.dm.api


object Parser {

	import org.xml.sax.SAXParseException

	def parseActionFile(node: scala.xml.Node): String = {
		node.text.split("#")(0)
	}

	def parseLocation(path: File, node: scala.xml.Node): api.Location = {
		val row = node.attribute("data-row").get.toString.toInt
		val col = node.attribute("data-col").get.toString.toInt
		api.Location(path.toString, Some(row), Some(col))
	}

	def parseDataOutEvent(path: File, node: scala.xml.Node): api.oozie.DataOutEvent = {
		api.oozie.DataOutEvent(
			location		= parseLocation(path, node)
			, name			= node.attribute("name").get.toString
			, datasetName	= node.attribute("dataset").get.toString
			, instance		= (node \ "instance").head.text
			, dataset		= None
		)
	}

	def parseDataInEvent(path: File, node: scala.xml.Node): api.oozie.DataInEvent = {
		val instList = (node \ "instance")
		val startInstList = (node \ "start-instance")
		val endInstList = (node \ "end-instance")
		api.oozie.DataInEvent(
			location		= parseLocation(path, node)
			, name			= node.attribute("name").get.toString
			, datasetName	= node.attribute("dataset").get.toString
			, instance		= if (instList.size > 0) instList.head.text else ""
			, startInstance	= if (startInstList.size > 0) startInstList.head.text else ""
			, endInstance	= if (endInstList.size > 0) endInstList.head.text else ""
			, dataset		= None
		)
	}

	def parseCoordinatorVariable(path: File, node: scala.xml.Node): api.Variable = {
		api.Variable(
			location	= parseLocation(path, node)
			, name		= (node \ "name").head.text.trim
			, value		= (node \ "value").head.text.trim
		)
	}

	def parseCoordinator(path: File, node: scala.xml.Node): api.oozie.Coordinator = {
		val datasets = (node \ "datasets" \ "dataset")
		val includes = (node \ "datasets" \ "include")
		val inputs = (node \ "input-events" \ "data-in").map(parseDataInEvent(path, _)).toList
		val outputs = (node \ "output-events" \ "data-out").map(parseDataOutEvent(path, _)).toList
		val variables = (node \ "action" \ "workflow" \ "configuration" \ "property").map(parseCoordinatorVariable(path, _)).toList
		api.oozie.Coordinator(
			location		= parseLocation(path, node)
			, name			= node.attribute("name").get.toString
			, start			= node.attribute("start").get.toString
			, end			= node.attribute("end").get.toString
			, timeout		= Some((node \ "controls" \ "timeout")(0).text)
			, includes		= includes.map(parseInclude(path, _)).toList
			, datasets		= datasets.map(parseDataset(path, _)).toList
			, inputs		= inputs
			, outputs		= outputs
			, variables		= variables	
			, wpath			= api.Location(file = (node \ "action" \ "workflow" \ "app-path")(0).text, row = None, col = None)
			, description	= Some("")
		)
	}

	def parseInclude(path: File, node: scala.xml.Node): api.oozie.Include = {
		api.oozie.Include(
			location = parseLocation(path, node).withFile(node.text)
		)
	}

	def parseWorkflow(path: File, node: scala.xml.Node): api.oozie.Workflow = {
		val actionFiles = (node \\ "file").map(parseActionFile).map(f => api.Location(file = f)).toList
		val varrx = "\\$\\{([a-zA-Z-_.]+)}".r
		val content = node.toString
		val variables = varrx.findAllMatchIn(content).map(m =>
			api.Variable(
				location = api.Location(file = path.toString, row = None, col = None)
				, name = m.group(1)
			)
		).toList
		api.oozie.Workflow(
			location	= parseLocation(path, node)
			, name		= ""
			, appPath	= ""
			, variables	= variables
			, files = actionFiles
		)
	}

	def parseDatabundle(path: File, node: scala.xml.Node): api.oozie.Databundle = {
		val datasets = (node \ "dataset").map{ ds => 
			parseDataset(path, ds)
		}.toList
		api.oozie.Databundle(
			location		= parseLocation(path, node)
			, name			= ""
			, datasets		= datasets
			, description	= ""
		)
	}

	def parseDataset(path: File, node: scala.xml.Node): api.oozie.Dataset = {
		api.oozie.Dataset(
			location			= parseLocation(path, node)
			, name				= node.attribute("name").get.toString
			, frequency			= node.attribute("frequency").get.toString
			, initialInstance	= node.attribute("initial-instance").get.toString
			, uri				= (node \ "uri-template")(0).text.toString
			, description		= ""
		)
	}

	def parse(path: File, root: scala.xml.Elem, ec: EntityCollector) = {

		def collectDatabundle(path: File, node: scala.xml.Elem, ec: EntityCollector): Unit = {
			val b = parseDatabundle(path, node)
			ec.collectDatabundle(b)
		}

		def collectCoordinator(path: File, node: scala.xml.Elem, ec: EntityCollector): Unit = {
			val c = parseCoordinator(path, node)
			ec.collectCoordinator(c)
		}

		def collectWorkflow(path: File, node: scala.xml.Elem, ec: EntityCollector): Unit = {
			val w = parseWorkflow(path, node)
			ec.collectWorkflow(w)
		}

		def collectError(path: File, node: scala.xml.Elem, e: SAXParseException, ec: EntityCollector): Unit = {
			ec.collectError(toError(path, e))
		}

		try {
			root.head.label match {
				case "datasets"			=> collectDatabundle(path, root, ec)
				case "workflow-app"		=> collectWorkflow(path, root, ec)
				case "coordinator-app"	=> collectCoordinator(path, root, ec)
			}
		} catch {
			case e: SAXParseException => collectError(path, root, e, ec)
		}
	}

	private def error(path: File, row: Option[Int], col: Option[Int], msg: String): api.XmlValidationError = {
		// TODO: Systematize error codes
		api.XmlValidationError(
			location = Some(api.Location(file = path.toString, row, col))
			, code = -1
			, kind = "xml parse error"
			, message = msg
		)
	}

	def toError(path: File, e: SAXParseException): api.XmlValidationError = {
		error(path, Some(e.getLineNumber), Some(e.getColumnNumber), e.getMessage)
	}

}
