package io.github.datamoth.dm.api.git

case class Commit(id: String
	, author: String
	, authorEmail: String
	, committer: String
	, committerEmail: String
	, msg: String
	, authorDateTime: String
	, commitDateTime: String
)
