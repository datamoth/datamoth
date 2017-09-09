package io.github.datamoth.dm.api
package oozie


case class Databundle(
	location: Location
	, name: String
	, description: String
	, datasets: Seq[Dataset]
)
