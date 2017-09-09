package io.github.datamoth.dm.api
package oozie


case class Coordinator(
	location: Location
	, name: String
	, start: String
	, end: String
	, timeout: Option[String]
	, datasets: List[Dataset]
	, includes: List[Include]
	, inputs: List[DataInEvent]
	, outputs: List[DataOutEvent]
	, variables: List[Variable]
	, description: Option[String]
	, wpath: Location
	, workflow: Option[Workflow]	= None
) {
	def withIncludes(includes: List[Include]): Coordinator = copy(includes = includes)
	def withInputs(inputs: List[DataInEvent]): Coordinator = copy(inputs = inputs)
	def withOutputs(outputs: List[DataOutEvent]): Coordinator = copy(outputs = outputs)
	def withWorkflow(workflow: Workflow): Coordinator = copy(workflow = Some(workflow))
}


object Coordinator {
	type Id = String
}
