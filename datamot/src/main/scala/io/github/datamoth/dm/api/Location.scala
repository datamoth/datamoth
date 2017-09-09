package io.github.datamoth.dm.api


case class Location(
	file: String
	, row: Option[Int] = None
	, col: Option[Int] = None
) {
	def withFile(file: String): Location = copy(file = file)
}
