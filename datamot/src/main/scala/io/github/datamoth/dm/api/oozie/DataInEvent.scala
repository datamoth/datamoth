package io.github.datamoth.dm.api
package oozie


case class DataInEvent(
	location: Location
	, name: String
	, datasetName: String
	, instance: String
	, startInstance: String
	, endInstance: String
	, dataset: Option[Dataset]
) {
	def withDataset(d: Option[Dataset]) = copy(dataset = d)
}
