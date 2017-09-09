package io.github.datamoth.dm.api

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory


case class Project(
	namespace: String
	, name: String
	, origin: String
	, remote: String
	, image: String
	, description: String		= ""
	, kind: String				= "oozie"
	, conf: Config				= ConfigFactory.empty
)
