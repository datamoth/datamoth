package io.github.datamoth.dm.imp

import java.io.File

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


object Location {

	private val brand = "datamot"

	case class Key(
		namespace: String
		, project: String
		, ref: String
		, profile: String
	)

	case class Config(
		repoDir: String
		, workDir: String
		, hdfsDir: String
	)

	def create(): Location = {
		val repoDir = new File(new File(System.getProperty("java.io.tmpdir"), brand), "repo")
		val workDir = new File(new File(System.getProperty("java.io.tmpdir"), brand), "work")
		val hdfsDir = "/tmp/datamot/work"
		val cfg = Config(repoDir = repoDir.toString, workDir = workDir.toString, hdfsDir = hdfsDir.toString)
		new Location(cfg = cfg)
	}

}

class Location(cfg: Location.Config) {

	def rtree(key: Location.Key): File = rtree(ns = key.namespace, pr = key.project)
	def wtree(key: Location.Key): File = wtree(ns = key.namespace, pr = key.project, rf = key.ref, pf = key.profile)
	def cache(key: Location.Key): File = cache(ns = key.namespace, pr = key.project, rf = key.ref, pf = key.profile)

	def rtree(ns: String, pr: String): File = {
		val d = new File(concat(workD, ns, pr), "rtree")
		return d
	}

	def wtree(ns: String, pr: String, rf: String, pf: String): File = {
		val d = new File(concat(workD, ns, pr, rf, pf), "wtree")
		return d
	}

	def cache(ns: String, pr: String, rf: String, pf: String): File = {
		val d = new File(concat(workD, ns, pr, rf, pf), "cache")
		return d
	}


	private val L = LoggerFactory.getLogger(classOf[Location])

	private val workD: File = new File(cfg.workDir)
	private val hdfsD: File = new File(cfg.hdfsDir)

	private def concat(root: File, ns: String, pr: String): File = {
		new File(new File(root, ns), pr)
	}

	private def concat(root: File, ns: String, pr: String, rf: String, pf: String): File = {
		new File(new File(new File(new File(root, ns), pr), rf), pf)
	}
}
