package io.github.datamoth.dm.api


case class VarNotFoundError(
	location: Option[Location]
	, code: Long
	, kind: String
	, message: String
) extends Error
