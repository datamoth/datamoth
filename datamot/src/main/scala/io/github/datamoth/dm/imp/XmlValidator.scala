package io.github.datamoth.dm.imp


object XmlValidator {

	import io.github.datamoth.dm.api

	import java.io.File
	import org.xml.sax.SAXParseException
	import javax.xml.validation.SchemaFactory
	import javax.xml.transform.stream.StreamSource
	import scala.collection.mutable.ArrayBuffer


	private def error(path: File, row: Option[Int], col: Option[Int], msg: String): api.XmlValidationError = {
		// TODO: Systematize error codes
		api.XmlValidationError(
			location = Some(api.Location(file = path.toString, row, col))
			, code = -1
			, kind = "xml validation error"
			, message = msg
		)
	}

	def toError(path: File, e: SAXParseException): api.XmlValidationError = {
		error(path, Some(e.getLineNumber), Some(e.getColumnNumber), e.getMessage)
	}

	def validate(root: File, path: File, namespace: String): List[api.Error] = {
		val errors = new ArrayBuffer[api.XmlValidationError]
		try {
			if (!schemas.contains(namespace)) {
				// errors.append(error(path, None, None, s"Can't find schema for namespace: ${namespace}"))
				// return errors.toSeq.toList
			} else {
				val schemaFile = schemas(namespace)
				val schemaLang = "http://www.w3.org/2001/XMLSchema"
				val factory = SchemaFactory.newInstance(schemaLang)
				val schema = factory.newSchema(new StreamSource(getClass.getClassLoader.getResourceAsStream(schemaFile)))
				val validator = schema.newValidator()
				val stream = new StreamSource(new File(root, path.toString))
				validator.validate(stream)
			}
		} catch {
			case e: SAXParseException => errors.append(toError(path, e))
		}
		return errors.toSeq.toList
	}

	def validate[T](root: File, path: File, namespace: String, onError: (api.Error) => T): List[api.Error] = {
		val errors = validate(root, path, namespace)
		errors.foreach(onError)
		errors
	}

	private val schemas = Map(
		"uri:oozie:coordinator:0.1" -> "oozie-coordinator-0.1.xsd",
		"uri:oozie:coordinator:0.2" -> "oozie-coordinator-0.2.xsd",
		"uri:oozie:coordinator:0.3" -> "oozie-coordinator-0.3.xsd",
		"uri:oozie:coordinator:0.4" -> "oozie-coordinator-0.4.xsd",
		"uri:oozie:coordinator:0.5" -> "oozie-coordinator-0.5.xsd",
		"uri:oozie:workflow:0.1" -> "oozie-workflow-0.1.xsd",
		"uri:oozie:workflow:0.2" -> "oozie-workflow-0.2.xsd",
		"uri:oozie:workflow:0.3" -> "oozie-workflow-0.3.xsd",
		"uri:oozie:workflow:0.4" -> "oozie-workflow-0.4.xsd",
		"uri:oozie:workflow:0.5" -> "oozie-workflow-0.5.xsd"
	)

}
