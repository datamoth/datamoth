package io.github.datamoth.dm.imp

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File
import java.security.PrivilegedExceptionAction

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

import org.apache.hadoop.fs.Path
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.FileStatus
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation

import scala.collection.JavaConverters._


object Spark {

	case class Config(user: String, sysopts: com.typesafe.config.Config)

	def create(user: String, sysopts: com.typesafe.config.Config): Spark = {
		new Spark(Config(user, sysopts))
	}

}

class Spark(cfg: Spark.Config) {

	private val L = LoggerFactory.getLogger(classOf[Spark])
	private val conf = createConf()

	private def createConf(): SparkConf = {
		val init = new SparkConf()
			.setAppName(s"Datamot Spark ${cfg.user}")
			.set("spark.master", "yarn")
			.set("spark.submit.deployMode", "client")
			.set("spark.hadoop.home.dir", "/tmp/")
		val opts = cfg.sysopts
		val conf = opts.entrySet.asScala.foldLeft(init) { case (conf, opt) =>
			conf.set(opt.getKey, opts.getString(opt.getKey))
		}
		conf
	}

	def test() {
		val sc = SparkContext.getOrCreate(conf)
		val dataD = sc.textFile("/user/sherzod/test").cache()
		val dataL = dataD.collect()
		dataL.foreach{ x => L.debug("************************ {}", x) }
		sc.stop()
	}

}
