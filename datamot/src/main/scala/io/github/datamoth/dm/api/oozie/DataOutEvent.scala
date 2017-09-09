package io.github.datamoth.dm.api
package oozie


case class DataOutEvent(
	location: Location
	, name: String
	, datasetName: String
	, instance: String
	, dataset: Option[Dataset]
) {
	def withDataset(d: Option[Dataset]) = copy(dataset = d)
}
