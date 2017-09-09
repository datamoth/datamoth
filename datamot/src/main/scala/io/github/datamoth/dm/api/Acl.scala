package io.github.datamoth.dm.api


case class NamespaceTeamAcl(
	namespace: Namespace
	, team: Team
)

case class ProjectTeamAcl(
	project: String
	, team: String
	, access: String
)

case class ProjectUserAcl(
	project: String
	, user: String
	, access: String
)

case class TeamUserAcl(
	team: String
	, user: String
	, access: String
)
