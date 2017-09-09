package io.github.datamoth.dm.api
package oozie


case class Dataset(
	location: Location
	, name: String
	, frequency: String
	, initialInstance: String
	, uri: String
	, description: String
)
