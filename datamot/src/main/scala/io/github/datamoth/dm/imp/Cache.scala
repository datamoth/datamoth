package io.github.datamoth.dm.imp


import java.io.File
import org.slf4j.Logger
import org.slf4j.LoggerFactory


// TODO: sanitize paths


object Cache {

	case class Key(
		namespace: String
		, project: String
		, ref: String
		, profile: String
	)

	case class Config(
		root: String
	)

	def create(root: String): Cache = {
		val cfg = Config(root = root)
		new Cache(cfg = cfg)
	}

	class Lock(private val lock: java.nio.channels.FileLock) extends java.io.Closeable {
		override def close(): Unit = {
			if (lock != null) {
				lock.close()
				lock.channel.close()
			}
		}
	}

}


class Cache(cfg: Cache.Config) {

	import io.github.datamoth.dm.api
	import Cache.Key

	def lock(key: Key): Cache.Lock = {
		import java.nio.file.Files
		import java.nio.file.Paths
		import java.nio.file.StandardOpenOption
		import java.nio.channels.FileLock
		import java.nio.channels.FileChannel
		import java.util.EnumSet
		projectD(key).mkdirs()
		val file = java.nio.file.Paths.get(s"${projectD(key)}/project.lock")
		val opts = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE)
		val chan = Files.newByteChannel(file, opts).asInstanceOf[FileChannel]
		new Cache.Lock(chan.lock())
	}

	def resolve(key: Key): Key = {
		val rfs = getRefs(key)
		key.copy(ref = rfs(key.ref))
	}

	def refD(k: Key): File = {
		new File(new File(new File(new File(cfg.root, k.namespace), k.project), "refs"), k.ref)
	}

	def profileD(k: Key): File = {
		new File(refD(k), k.profile)
	}

	def projectD(k: Key): File = {
		new File(new File(cfg.root, k.namespace), k.project)
	}

	def projectTreeD(k: Key): File = {
		new File(profileD(resolve(k)), "tree")
	}

	def projectRepoD(k: Key): File = {
		new File(new File(new File(cfg.root, k.namespace), k.project), "repo")
	}

	def namespaceD(k: Key): File = {
		new File(cfg.root, k.namespace)
	}

	def refCache(k: Key): File = {
		new File(projectD(k), "refcache.json")
	}

	def profileCache(k: Key): File = {
		new File(refD(k), "profiles.json")
	}

	def projectCache(k: Key): File = {
		new File(profileD(k), "project.json")
	}

	def deployCache(k: Key): File = {
		new File(profileD(k), "deploy.json")
	}

	def commitLogCache(k: Key): File = {
		new File(refD(k), "clog.json")
	}


	def getRefs(key: Key): Map[String, String] = {
		val k = key.copy(ref = "*", profile = "*")
		if (refCache(k).exists()) {
			readRefs(k)
		} else {
			Map()
		}
	}

	def getProfiles(key: Key): List[api.Profile] = {
		val k = resolve(key).copy(profile = "*")
		if (profiles.contains(k.toString)) {
			return profiles(k.toString)
		}
		if (profileCache(k).exists()) {
			profiles.clear()
			profiles(k.toString) = readProfiles(k)
			profiles(k.toString)
		} else {
			throw new java.util.NoSuchElementException(s"No profiles cached for ${k}")
		}
	}

	def getProject(key: Key): api.oozie.Project = {
		val k = resolve(key)
		if (projects.contains(k.toString)) {
			return projects(k.toString)
		}
		if (projectCache(k).exists()) {
			readProject(k)
		} else {
			throw new java.util.NoSuchElementException(s"No project cached for ${k}")
		}
	}

	def getDeploy(key: Key): api.oozie.Project = {
		val k = resolve(key)
		if (deploys.contains(k.toString)) {
			return deploys(k.toString)
		}
		if (deployCache(k).exists()) {
			readDeploy(k)
		} else {
			throw new java.util.NoSuchElementException(s"No deployed project cached for ${k}")
		}
	}

	def getCommitLog(key: Key): List[api.git.Commit] = {
		val k = resolve(key)
		if (clogs.contains(k.toString)) {
			return clogs(k.toString)
		}
		if (commitLogCache(k).exists()) {
			readCommitLog(k)
		} else {
			throw new java.util.NoSuchElementException(s"No commit log cached for ${k}")
		}
	}

	def putRefs(key: Key, rfs: Map[String, String]) {
		val k = key.copy(ref = "*", profile = "*")
		writeRefs(k, rfs)
	}

	def putProfiles(key: Key, pfs: List[api.Profile]) {
		val k = key.copy(profile = "*")
		writeProfiles(k, pfs)
		profiles(k.toString) = pfs
	}

	def putProject(key: Key, project: api.oozie.Project): Unit = {
		val k = resolve(key)
		writeProject(k, project)
		projects(k.toString) = project
	}

	def putDeploy(key: Key, project: api.oozie.Project): Unit = {
		val k = key
		writeDeploy(key, project)
		deploys(k.toString) = project
	}

	def putCommitLog(key: Key, clog: List[api.git.Commit]): Unit = {
		val k = resolve(key)
		writeCommitLog(k, clog)
		clogs(k.toString) = clog
	}


	def reset(key: Key, ref: String, commitId: String): Unit = {
		val k = key.copy(ref = "*", profile = "*")
		val refs = if (refCache(k).exists()) { readRefs(k) } else { Map.empty[String, String] }
		val newrfs = (refs - ref) + (ref -> commitId) + (commitId -> commitId)
		writeRefs(k, newrfs)
	}


	// Assume rf (ref) is always resolved to commit id

	private val L = LoggerFactory.getLogger(classOf[Cache])

	private val profiles = scala.collection.mutable.HashMap[String, List[api.Profile]]()
	private val projects = scala.collection.mutable.HashMap[String, api.oozie.Project]()
	private val deploys = scala.collection.mutable.HashMap[String, api.oozie.Project]()
	private val clogs = scala.collection.mutable.HashMap[String, List[api.git.Commit]]()


	import java.io.PrintWriter
	import org.apache.commons.io.FileUtils

	import com.fasterxml.jackson.databind.{KeyDeserializer, JsonSerializer}
	import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializerProvider}
	import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
	import com.fasterxml.jackson.module.scala.DefaultScalaModule
	import com.fasterxml.jackson.core.JsonGenerator


	val mapper = new ObjectMapper() with ScalaObjectMapper
	mapper.registerModule(DefaultScalaModule)
	mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)


	private def write(value: Any): String = {
		mapper.writeValueAsString(value)
	}

	private def read[T](json: String)(implicit m : Manifest[T]): T = {
		mapper.readValue[T](json)
	}


	private def readFile(f: File): String = {
		scala.io.Source.fromFile(f).mkString
	}

	private def readProfiles(key: Key): List[api.Profile] = {
		val file = profileCache(key)
		val body = readFile(file)
		read[List[api.Profile]](body)
	}

	private def writeProfiles(key: Key, pfs: List[api.Profile]): Unit = {
		val file = profileCache(key)
		refD(key).mkdirs()
		FileUtils.deleteQuietly(file)
		AsResource(new PrintWriter(file)).map { out =>
			val body = write(pfs)
			out.write(body)
		}
	}

	private def readRefs(key: Key): Map[String, String] = {
		val file = refCache(key)
		val body = readFile(file)
		read[Map[String, String]](body)
	}

	private def writeRefs(key: Key, refs: Map[String, String]): Unit = {
		val file = refCache(key)
		projectD(key).mkdirs()
		FileUtils.deleteQuietly(file)
		AsResource(new PrintWriter(file)).map { out =>
			val body = write(refs)
			out.write(body)
		}
	}

	private def readProject(key: Key): api.oozie.Project = {
		val file = projectCache(key)
		val body = readFile(file)
		read[api.oozie.Project](body)
	}

	private def writeProject(key: Key, project: api.oozie.Project): Unit = {
		val file = projectCache(key)
		profileD(key).mkdirs()
		FileUtils.deleteQuietly(file)
		AsResource(new PrintWriter(file)).map { out =>
			val body = write(project)
			out.write(body)
		}
	}

	private def readDeploy(key: Key): api.oozie.Project = {
		val file = deployCache(key)
		val body = readFile(file)
		read[api.oozie.Project](body)
	}

	private def writeDeploy(key: Key, project: api.oozie.Project): Unit = {
		val file = deployCache(key)
		profileD(key).mkdirs()
		FileUtils.deleteQuietly(file)
		AsResource(new PrintWriter(file)).map { out =>
			val body = write(project)
			out.write(body)
		}
	}

	private def readCommitLog(key: Key): List[api.git.Commit] = {
		val file = commitLogCache(key)
		val body = readFile(file)
		read[List[api.git.Commit]](body)
	}

	private def writeCommitLog(key: Key, clog: List[api.git.Commit]): Unit = {
		val file = commitLogCache(key)
		refD(key).mkdirs()
		FileUtils.deleteQuietly(file)
		AsResource(new PrintWriter(file)).map { out =>
			val body = write(clog)
			out.write(body)
		}
	}

	class ConfigSerializer extends JsonSerializer[com.typesafe.config.Config] {
		import com.typesafe.config.ConfigRenderOptions
		override def serialize(conf: com.typesafe.config.Config, jgen: JsonGenerator, provider: SerializerProvider) = {
			val dcontent = conf.root().render(ConfigRenderOptions.concise.setFormatted(true))
			jgen.writeStartObject();
			jgen.writeEndObject();
		}
	}

}
