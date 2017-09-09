package io.github.datamoth.dm.api
package oozie


case class Include(
	location: Location
	, databundle: Option[Databundle]	= None
) {
	def withDatabundle(b: Databundle) = copy(databundle = Some(b))
}
