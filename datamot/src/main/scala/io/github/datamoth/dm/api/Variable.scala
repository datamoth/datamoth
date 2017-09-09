package io.github.datamoth.dm.api


case class Variable(
	location: Location
	, name: String
	, value: String		= ""
)
