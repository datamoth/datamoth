package io.github.datamoth.dm.imp


object XmlLoader extends scala.xml.factory.XMLLoader[scala.xml.Elem] {
	trait WithLocation extends scala.xml.parsing.NoBindingFactoryAdapter {
		import org.xml.sax.{helpers, Locator, SAXParseException}
		import scala.xml._
		var locator: org.xml.sax.Locator = _
		abstract override def setDocumentLocator(locator: Locator) {
			this.locator = locator
			super.setDocumentLocator(locator)
		}
		abstract override def warning(e: SAXParseException) {
			super.warning(e)
		}
		abstract override def error(e: SAXParseException) {
			super.error(e)
		}
		abstract override def fatalError(e: SAXParseException) {
			super.fatalError(e)
		}
		abstract override def createNode(pre: String, label: String, attrs: MetaData, scope: NamespaceBinding, children: List[Node]): Elem = {
			val node = super.createNode(pre, label, attrs, scope, children) %
						Attribute("data-row", Text(locator.getLineNumber.toString), Null) %
						Attribute("data-col", Text(locator.getColumnNumber.toString), Null)
			node
		}
	}
	override def adapter = new scala.xml.parsing.NoBindingFactoryAdapter with WithLocation
}
