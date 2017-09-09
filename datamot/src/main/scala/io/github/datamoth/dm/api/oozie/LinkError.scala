package io.github.datamoth.dm.api
package oozie


case class LinkError(
	location: Option[Location]
	, code: Long			= -1
	, kind: String			= "oozie link error"
	, message: String
) extends Error
