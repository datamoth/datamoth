package io.github.datamoth.dm.api
package oozie

import java.io.File


case class Project(
	meta: io.github.datamoth.dm.api.Project
	, coordinators	: List[Coordinator]
	, databundles	: List[Databundle]
	, workflows		: List[Workflow]
	, datasets		: List[Dataset]
	, plugins		: List[Plugin]
	, errors		: List[Error]
	, warnings		: List[Error]
	, files			: List[Location]
	, hueUrl		: Option[String]		= None
	, deploy		: Option[DeployInfo]	= None
	, _workDir		: File					= null
) {
	def withCoordinators(coordinators: List[Coordinator]): Project = copy(coordinators = coordinators)
	def withDeploy(di: DeployInfo): Project = copy(deploy = Some(di))
	def withErrors(errors: List[Error]): Project = copy(errors = errors)
}
