package io.github.datamoth.dm.api
package oozie


case class Workflow(
	location: Location
	, name: String
	, appPath: String
	, variables: List[Variable]
	, files: List[Location]
)
