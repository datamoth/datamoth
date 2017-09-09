package io.github.datamoth.dm.imp


import java.io.File
import com.typesafe.config.Config
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import com.typesafe.config.ConfigRenderOptions


object Z {

	import scala.collection.JavaConverters._


	def loadConfig: Config = {
		val path = new File(Z.getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath)
						.getParentFile
						.getParentFile
		ConfigFactory.parseFile(new File(new File(path, "conf"), "datamot.conf")).withFallback(ConfigFactory.load).resolve
	}

	def parse(conf: File): Config = {
		ConfigFactory.parseFile(conf).resolve
	}

	def parse(conf: String, fb: Config = Z.emptyConf): Config = {
		ConfigFactory.parseString(conf).withFallback(fb).resolve
	}

	def render(conf: Config): String = {
		conf.root().render(ConfigRenderOptions.concise.setFormatted(true))
	}

	def emptyMap: ConfigValue = ConfigValueFactory.fromMap(Map.empty[String, Object].asJava)
	def emptyConf: Config = ConfigFactory.empty
	def emptyConf(defaults: (String, AnyRef)*): Config = defaults.foldLeft(emptyConf){
		case (c, d) => d match {
			case (k, v) => c.withValue(k, ConfigValueFactory.fromAnyRef(v))
		}
	}

}
