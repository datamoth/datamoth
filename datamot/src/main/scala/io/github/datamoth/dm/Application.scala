package io.github.datamoth.dm

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import scala.collection.JavaConverters._


object Application {

	def main(args: Array[String]): Unit = {
		val user = System.getProperty("user.name")
		if (args(0) == "--test") {
			val dm = Datamot.create(user = user)
			dm.test()
		} else if (args(0) == "--init") {
			val dm = Datamot.create(user = user)
			dm.init()
		} else if (args(0) == "--start") {
			val dm = Datamot.create(user = user)
			dm.init()
			val webServer = new WebServer
			webServer.start(dm.cfg.host, dm.cfg.port)
		} else if (args(0) == "--compile") {
			val dm = Datamot.create(user = user)
			val workdir		= args(1)
			val namespace	= args(2)
			val project		= args(3)
			val ref			= args(4)
			val commit		= args(5)
			dm.compile(workdir, namespace, project, ref, commit)
		} else if (args(0) == "--deploy") {
			val dm = Datamot.create(user = user)
			val namespace	= args(1)
			val project		= args(2)
			val ref			= args(3)
			val commit		= args(4)
			dm.deploy(namespace, project, ref, commit)
		} else if (args(0) == "--info") {
			val dm = Datamot.create(user = user)
			dm.cfg.projects.foreach{ p =>
				println(s"${p.namespace}/${p.name}")
			}
		} else if (args(0) == "--version") {
			println("datamot version babymot")
		}
	}

}
