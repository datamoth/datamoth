package io.github.datamoth.dm.api
package oozie

object DeployError {
	def from(msg: String): DeployError = {
		DeployError(location = None, code = -1, kind = "deploy error", message = msg)
	}
	def from(e: Exception): DeployError = {
		DeployError(location = None, code = -1, kind = "deploy error", message = e.getMessage)
	}
}

case class DeployError(
	location: Option[Location]
	, code: Long
	, kind: String
	, message: String
) extends Error

case class DeployCommand(
	coordinatorName: String
	, id: Option[Coordinator.Id]		= None
	, coordinator: Option[Coordinator]	= None
	, actions: Seq[String]				= Seq()
	, errors: List[DeployError]			= List()
	, status: Option[String]			= None
) {
	// TODO: Use enums for commands
	def withId(id: String): DeployCommand = copy(id = Some(id))
	def withoutId(): DeployCommand = copy(id = None)
	def withError(e: DeployError): DeployCommand = copy(errors = errors :+ e)
	def withError(msg: String): DeployCommand = withError(
		DeployError(location = None, code = -1, kind = "deploy error", message = s"${coordinatorName}: ${msg}")
	)
	def withStatus(status: String) = copy(status = Some(status))
	def withoutStatus = copy(status = None)
}

case class DeployInfo(
	commands: Seq[DeployCommand]	= Seq()
	, done: Boolean
	, autoDeploy: Profile
	, errors: List[DeployError]
	, oldErrors: List[DeployError]
	, appdir: Option[String]		= None
) {
	def setCommands(commands: Seq[DeployCommand]) = copy(commands = commands)
	def setErrors(errors: List[DeployError]) = copy(errors = errors)
	def completed: DeployInfo = copy(done = true, oldErrors = List())
	def withCommandErrors: DeployInfo = {
		copy(errors = errors ++ commands.map(_.errors).toList.flatten)
	}
	def withAppdir(appdir: String): DeployInfo = copy(appdir = Some(appdir))
}
