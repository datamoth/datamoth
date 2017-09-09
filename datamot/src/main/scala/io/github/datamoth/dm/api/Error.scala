package io.github.datamoth.dm.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonSubTypes.Type


@JsonTypeInfo(
	use=JsonTypeInfo.Id.NAME,
	include=JsonTypeInfo.As.PROPERTY,
	property="name"
)@JsonSubTypes(Array(
	new Type(value=classOf[VarNotFoundError], name="var-not-found-error"),
	new Type(value=classOf[XmlValidationError], name="xml-validation-error"),
	new Type(value=classOf[oozie.LinkError], name="oozie-link-error")
))
trait Error {
	val location: Option[Location]
	val code: Long
	val kind: String
	val message: String
}
