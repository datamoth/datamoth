package io.github.datamoth.dm

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigValueFactory
import scala.collection.JavaConverters._

import java.io.File
import java.nio.file.Files
import java.io.PrintWriter
import java.util.Random
import java.time.ZoneId
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


object Datamot {
	
	class Config(val user: String, val conf: com.typesafe.config.Config) {

		import io.github.datamoth.dm.api
		import io.github.datamoth.dm.imp
		import io.github.datamoth.dm.imp.Z
		import scala.collection.JavaConverters._

		def remoteUser = conf.getString("datamot.remoteUser")
		def remoteHost = conf.getString("datamot.remoteHost")

		def host = conf.getString("datamot.host")
		def port = conf.getInt("datamot.port")

		def poems: IndexedSeq[String]		= conf.getStringList("datamot.speech.poems").asScala.toIndexedSeq
		def succPhrases: IndexedSeq[String]	= conf.getStringList("datamot.speech.succ").asScala.toIndexedSeq
		def failPhrases: IndexedSeq[String]	= conf.getStringList("datamot.speech.fail").asScala.toIndexedSeq

		def workDir = conf.getString("datamot.workdir")
		def hdfsDir = conf.getString("datamot.hdfsdir")

		def namespaces: List[api.Namespace] = {
			parseNamespaces(conf)
		}

		def projects: List[api.Project] = {
			parseProjects(conf)
		}


		def getProject(namespace: String, name: String): api.Project = {
			val projects = parseProjects(conf)
			projects.filter(p => p.namespace == namespace && p.name == name).head
		}


		private def parseNamespace(c: com.typesafe.config.Config): api.Namespace = {
			val conf = c.withFallback(Z.emptyConf("image" -> "", "description" -> ""))
			api.Namespace(
				name = c.getString("name")
				, image = c.getString("image")
				, description = c.getString("description")
			)
		}

		private def parseNamespaces(c: com.typesafe.config.Config): List[api.Namespace] = {
			c.getConfigList("datamot.namespaces").asScala.map(parseNamespace).toList
		}

		private def parseProject(c: com.typesafe.config.Config): api.Project = {
			val conf = c.withFallback(Z.emptyConf("image" -> "", "description" -> "", "kind" -> "oozie", "conf" -> Z.emptyMap))
			api.Project(
				name = c.getString("name")
				, namespace = c.getString("namespace")
				, origin = c.getString("origin")
				, remote = s"${remoteUser}@${remoteHost}:${c.getString("origin")}"
				, kind = c.getString("origin")
				, image = c.getString("image")
				, description = c.getString("image")
			)
		}

		private def parseProjects(c: com.typesafe.config.Config): List[api.Project] = {
			c.getConfigList("datamot.projects").asScala.map(parseProject).toList
		}

		private def parseProjectUserAclItem(c: com.typesafe.config.Config): api.ProjectUserAcl = {
			api.ProjectUserAcl(
				project = c.getString("project")
				, user = c.getString("user")
				, access = c.getString("access")
			)
		}

		private def parseProjectTeamAclItem(c: com.typesafe.config.Config): api.ProjectTeamAcl = {
			api.ProjectTeamAcl(
				project = c.getString("project")
				, team = c.getString("team")
				, access = c.getString("access")
			)
		}

		private def parseTeamUserAclItem(c: com.typesafe.config.Config): api.TeamUserAcl = {
			api.TeamUserAcl(
				team = c.getString("team")
				, user = c.getString("user")
				, access = c.getString("access")
			)
		}

		private def parseProjectUserAcl(c: com.typesafe.config.Config): List[api.ProjectUserAcl] = {
			c.getConfigList("datamot.project-user").asScala.map(parseProjectUserAclItem).toList
		}

		private def parseProjectTeamAcl(c: com.typesafe.config.Config): List[api.ProjectTeamAcl] = {
			c.getConfigList("datamot.project-team").asScala.map(parseProjectTeamAclItem).toList
		}

		private def parseTeamUserAcl(c: com.typesafe.config.Config): List[api.TeamUserAcl] = {
			c.getConfigList("datamot.team-user").asScala.map(parseTeamUserAclItem).toList
		}

	}

	def create(user: String): Datamot = {
		val conf = imp.Z.loadConfig
		new Datamot(new Config(user = user, conf))
	}

}

class Datamot(val cfg: Datamot.Config) {

	import imp.Z
	import imp.Cache
	import imp.Location
	import imp.Renderer
	import imp.Repository
	import imp.AsResource
	import imp.oozie

	private val loc = Location.create()
	private val cache = Cache.create(root = cfg.workDir)
	private val map = cfg.projects.groupBy(p => (p.namespace, p.name)).map{ case (k, v) => (k, v.head) }.toMap
	private val rand = new Random(System.currentTimeMillis())

	private val L = LoggerFactory.getLogger(classOf[Datamot])

	def namespaces = cfg.namespaces
	def projects = cfg.projects


	def test(): Unit = {
		val key = Cache.Key(namespace = "datamot", project = "babymot", ref = "master", profile = "production")
		val conf = Z.parse(readFile(cache.resolve(key), ".conf"), sysvars(key))
		val spark = imp.Spark.create(cfg.user, conf.getConfig("sysoptions.spark"))
		spark.test()
	}

	def init(): Unit = {
		cfg.projects.foreach { p =>
			val repd = new File(p.origin)
			if (!repd.exists()) {
				L.info("Initialize project remote repository: {}/{} in {}", p.namespace:Object, p.name, p.origin)
				Repository.init(p.namespace, p.name, cfg.workDir, repd)
			}
		}
	}

	/* !!! This method is only supposed to be called within git update hook !!! */
	def compile(workdir: String, namespace: String, project: String, ref: String, commit: String): Unit = {
		compile(new File(workdir), namespace, project, ref, commit)
	}

	def getOozieCoordinatorInfo(namespace: String, project: String, ref: String, profile: String, name: String): List[AnyRef] = {
		val key = Cache.Key(namespace = namespace, project = project, ref = ref, profile = profile)
		val conf = Z.parse(readFile(cache.resolve(key), ".conf"), sysvars(key))
		val hdfd = mkHdfsPath(key)
		val oozi = imp.oozie.Client.create(cfg.user, hdfd, conf.getConfig("sysoptions.oozie"))
		oozi.getCoordinatorInfo(name)
	}

	def getOozieJobs(namespace: String, project: String, ref: String, profile: String, maxCount: Int): List[AnyRef] = {
		val key = Cache.Key(namespace = namespace, project = project, ref = ref, profile = profile)
		val conf = Z.parse(readFile(cache.resolve(key), ".conf"), sysvars(key))
		val hdfd = mkHdfsPath(key)
		val oozi = imp.oozie.Client.create(cfg.user, hdfd, conf.getConfig("sysoptions.oozie"))
		oozi.getJobs(maxCount)
	}

	def hdfsLs(namespace: String, project: String, ref: String, profile: String, path: String): List[AnyRef] = {
		val key = Cache.Key(namespace = namespace, project = project, ref = ref, profile = profile)
		val conf = Z.parse(readFile(cache.resolve(key), ".conf"), sysvars(key))
		val hdfs = imp.Hdfs.create(cfg.user, conf.getConfig("sysoptions.hdfs"))
		hdfs.ls(path)
	}

	def deploy(namespace: String, project: String, ref: String, commitId: String): Unit = {
		val key = Cache.Key(namespace = namespace, project = project, ref = ref, profile = "*")
		L.debug("Try lock key: {}", key)
		AsResource(cache.lock(key)).map { lock =>
			L.debug("Done lock key: {}", key)
			cache.getProfiles(key).foreach { prof =>
				val profileKey = key.copy(profile = prof.name)
				val compiledProject = cache.getProject(profileKey)
				compiledProject.deploy.foreach{ d =>
					if (!d.done && d.autoDeploy.name == prof.name) {
						deploy(profileKey)
					}
				}
			}
		}
	}

	def getRefs(namespace: String, project: String): List[api.git.Ref] = {
		val key = Cache.Key(namespace = namespace, project = project, ref = "*", profile = "*")
		AsResource(cache.lock(key)).map { lock =>
			cache.getRefs(key).map{ case (k, v) => api.git.Ref(id = v, name = k) }.filter(x => x.id != x.name).toList
		}
	}

	def getProfiles(namespace: String, project: String, ref: String): List[api.Profile] = {
		val key = Cache.Key(namespace = namespace, project = project, ref = ref, profile = "*")
		AsResource(cache.lock(key)).map { lock =>
			cache.getProfiles(key)
		}
	}

	def getProject(namespace: String, project: String, ref: String, profile: String): api.oozie.Project = {
		val key = Cache.Key(namespace = namespace, project = project, ref = ref, profile = profile)
		AsResource(cache.lock(key)).map { lock =>
			cache.getProject(key)
		}
	}

	def getCommitLog(namespace: String, project: String, ref: String): List[api.git.Commit] = {
		val key = Cache.Key(namespace = namespace, project = project, ref = ref, profile = "*")
		AsResource(cache.lock(key)).map { lock =>
			cache.getCommitLog(key)
		}
	}

	def getFile(namespace: String, project: String, ref: String, profile: String, path: String): api.Content = {
		// TODO: sanitize path
		val key = Cache.Key(namespace = namespace, project = project, ref = ref, profile = profile)
		val content = AsResource(cache.lock(key)).map { lock => readFile(key, path) }
		api.Content(content = content, location = api.Location(path))
	}

	def getFiles(namespace: String, project: String, ref: String, profile: String, paths: List[String]): List[api.Content] = {
		// TODO: sanitize path
		val key = Cache.Key(namespace = namespace, project = project, ref = ref, profile = profile)
		val contents = AsResource(cache.lock(key)).map { lock => paths.map(p => readFile(key, p)).toList }
		contents.zipWithIndex.map{case (c, i) => api.Content(content = c, location = api.Location(paths(i)))}.toList
	}


	private def sysvars(key: Cache.Key): Config = {
		// TODO: sanitize paths
		Z.emptyConf(
			"PROJECT_DIR"				-> mkHdfsPath(key)

			, "CURRENT_NAMESPACE"		-> key.namespace
			, "CURRENT_PROJECT"			-> key.project
			, "CURRENT_REF"				-> key.ref
			, "CURRENT_PROFILE"			-> key.profile
			, "CURRENT_USER"			-> cfg.user

			, "FAR_FUTURE_DATE"			-> DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now.plusYears(100))
			, "FAR_FUTURE_DATETIME"		-> DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").format(LocalDateTime.now.plusYears(100))
			, "FAR_FUTURE_GMT_DATE"		-> DateTimeFormatter.ofPattern("yyyy-MM-dd").format(ZonedDateTime.now(ZoneId.of("GMT")).plusYears(100))
			, "FAR_FUTURE_GMT_DATETIME"	-> DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").format(ZonedDateTime.now(ZoneId.of("GMT")).plusYears(100))

			, "DEPLOY_DATE"				-> DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now)
			, "DEPLOY_DATETIME"			-> DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").format(LocalDateTime.now)
			, "DEPLOY_GMT_DATE"			-> DateTimeFormatter.ofPattern("yyyy-MM-dd").format(ZonedDateTime.now(ZoneId.of("GMT")))
			, "DEPLOY_GMT_DATETIME"		-> DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").format(ZonedDateTime.now(ZoneId.of("GMT")))

			, "_rnd_alnum_5_"			-> "_rnd_alnum_5_"
			, "_rnd_alnum_10_"			-> "_rnd_alnum_10_"
			, "_rnd_alnum_15_"			-> "_rnd_alnum_15_"
			, "_rnd_alnum_20_"			-> "_rnd_alnum_20_"
		)
	}

	private def readFile(key: Cache.Key, path: String): String = {
		// TODO: sanitize path
		import java.io.File
		val dir = cache.projectTreeD(key)
		val file = new File(dir, path)
		scala.io.Source.fromFile(file).mkString
	}

	private def compile(repoDir: File, namespace: String, project: String, ref: String, commit: String): Unit = {

		def readProfiles(): List[api.Profile] = {
			val cnf = Z.parse(new File(repoDir, ".conf"))
			cnf.getConfigList("profiles").asScala.map { c =>
				api.Profile(c.getString("name"), c.getString("description"))
			}.toList
		}

		val key = Cache.Key(namespace = namespace, project = project, ref = ref, profile = "*")
		val projectKey = key.copy(ref = "*", profile = "*")
		val metaProject = map(projectKey.namespace, projectKey.project)

		val repo = Repository.create(new File("."), metaProject.origin)

		val cid = commit
		cache.reset(projectKey, key.ref, cid)

		val refKey = projectKey.copy(ref = cid)
		val profiles = readProfiles()
		cache.putProfiles(refKey, profiles)

		val clog = repo.commitLog(commit, 32)
		cache.putCommitLog(key, clog.toList)

		profiles.foreach{ p =>
			val profileKey = refKey.copy(profile = p.name)
			val workDir = cache.projectTreeD(profileKey)

			if (workDir.exists()) workDir.delete()
			workDir.mkdirs()

			val ec = new imp.oozie.EntityCollector
			val rend = imp.Renderer.create(sysvars = sysvars(profileKey))

			val rres = rend.render(repoDir, workDir, p.name)

			rres.errors.foreach(ec.collectError(_))
			rres.plugins.foreach(ec.collectPlugin(_))
			rres.files.foreach(ec.collectFile(_))

			// Compile
			import org.xml.sax.SAXParseException
			import java.io.File

			imp.Walker.walk(workDir, (f: File) => !f.getName.startsWith("."), { file: File =>
				if (file.getName.endsWith(".xml")) {
					try {
						val xml = imp.XmlLoader.loadFile(new File(workDir, file.toString))
						imp.oozie.Parser.parse(file, xml, ec)
						imp.XmlValidator.validate(workDir, file, xml.namespace, ec.collectError(_))
					} catch {
						case e: SAXParseException => ec.collectError(imp.XmlValidator.toError(file, e))
					}
				}
			})

			val pconf = Z.parse(readFile(profileKey, ".conf"), sysvars(profileKey))

			val projectAST_1 = api.oozie.Project(
				meta			= null,
				coordinators	= ec.coordinators,
				databundles		= ec.databundles,
				workflows		= ec.workflows,
				datasets		= ec.datasets,
				plugins			= ec.plugins,
				errors			= ec.errors,
				warnings		= ec.warnings,
				files			= ec.files,
				hueUrl			= if (pconf.hasPath("sysoptions.hue.url")) Some(pconf.getString("sysoptions.hue.url")) else None,
				_workDir		= workDir
			)

			val dconf = Z.parse(readFile(profileKey, ".deploy.conf"), sysvars(profileKey))
			val dinfo = oozie.Deployer.parseDeployInfo(dconf, projectAST_1)

			val projectAST_2 = imp.oozie.Linker.link(projectAST_1.copy(deploy = Some(dinfo)), ec)
			val projectAST_3 = projectAST_2.copy(warnings = ec.warnings, errors = ec.errors)

			if (projectAST_3.warnings.size > 0) projectAST_3.warnings.foreach(e => L.warn("Project warning: {}", e))
			if (projectAST_3.errors.size > 0) projectAST_3.errors.foreach(e => L.error("Project error: {}", e))

			cache.putProject(profileKey, projectAST_3)
		}
	}

	private def deploy(key: Cache.Key): Unit = {
		val metaProject = map(key.namespace, key.project)
		val repoDir = cache.projectRepoD(key)
		val repo = Repository.create(new File(repoDir, ".git"), metaProject.origin)

		repo.sync()
		repo.checkoutAndTrack(s"refs/remotes/origin/${key.ref}")

		val conf = Z.parse(readFile(key, ".conf"), sysvars(key))
		val hdfs = imp.Hdfs.create(cfg.user, conf.getConfig("sysoptions.hdfs"))
		val hdfd = mkHdfsPath(key)
		val oozi = imp.oozie.Client.create(cfg.user, hdfd, conf.getConfig("sysoptions.oozie"))
		val wrkd = cache.projectTreeD(key)
		val p = cache.getProject(key)
		L.info("Going to deploy: {}", key)
		if (p.errors.size > 0) {
			L.error("Project has errors, quit deploy")
			p.errors.foreach(e => L.error("Project error: {}", e))
			return
		}
		p.deploy.map{ d =>
			if (!d.errors.isEmpty) {
				d.errors.foreach(e => L.error("Deploy error, {}", e.message))
			} else if (!d.done) {
				hdfs.upload(wrkd, hdfd)
				val newd = oozi.deploy(d).completed
				val newdconf = oozie.Deployer.deploy2conf(newd)
				commit(key, newdconf, newd.errors.size != 0)
				val deployedProject = p.withDeploy(newd)
				cache.putProject(key, deployedProject)
			} else {
				L.info("Nothing to deploy, so do nothing, yeah...")
			}
		}.head
	}

	private def mkHdfsPath(key: Cache.Key): String = {
		// TODO: sanitize key
		s"${cfg.hdfsDir}/${key.namespace}/${key.project}/${cache.resolve(key).ref}/${key.profile}"
	}

	private def commit(key: Cache.Key, dconf: Config, hasErrors: Boolean) {
		val metaProject = map(key.namespace, key.project)
		val repoDir = cache.projectRepoD(key)
		val repo = Repository.create(repoDir, metaProject.origin)
		val dfile = new File(repoDir, ".deploy.conf")
		L.debug("Saving new state and clearing deploy file")
		val dcontent = dconf.root().render(ConfigRenderOptions.concise.setFormatted(true))
		imp.AsResource(new PrintWriter(dfile, "UTF-8")).foreach{ w => w.print(dcontent) }
		val msg = if (hasErrors) {
			s"${randomFail}<br /><br />${randomPoem}"
		} else {
			s"${randomSucc}<br /><br />${randomPoem}"
		}
		repo.commitTagAndPush("deployed", msg)
	}

	private def randomPoem: String = {
		val random_index = rand.nextInt(cfg.poems.size)
		cfg.poems(random_index)
	}

	private def randomSucc: String = {
		val random_index = rand.nextInt(cfg.succPhrases.size)
		cfg.succPhrases(random_index)
	}

	private def randomFail: String = {
		val random_index = rand.nextInt(cfg.failPhrases.size)
		cfg.failPhrases(random_index)
	}

}
