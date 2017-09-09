package io.github.datamoth.dm.imp.oozie

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config
import com.typesafe.config.ConfigUtil
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigValueFactory
import com.typesafe.config.ConfigRenderOptions

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer


object Deployer {

	import io.github.datamoth.dm.api

	private val L = LoggerFactory.getLogger(classOf[Client])

	def parseDeployInfo(deployConf: Config, project: api.oozie.Project): api.oozie.DeployInfo = {
		case class IdStatus(id: Option[api.oozie.Coordinator.Id], status: Option[String]) 
		// TODO: Use enums for commands
		val done = parseDoneFlag(deployConf)
		val autoDeploy = parseAutoDeploy(deployConf)
		val errs = parseErrors(deployConf)
		val dconf: Config = try {
			deployConf.getConfig("deploy.oozie.coordinators")
		} catch {
			case e: Exception =>
				return api.oozie.DeployInfo(done = done, autoDeploy = autoDeploy, errors = List(api.oozie.DeployError.from(e)), oldErrors = errs)
		}
		val cmap = project.coordinators.map{ c => (c.name, c) }.toMap
		val commands = dconf.root.entrySet.asScala.map{ item =>
			val cname = item.getKey
			val actions = dconf.root.get(cname).unwrapped.toString.split(',').map(_.trim).toList
			val id = None
			val status = None
			val coordinator = cmap.get(cname)
			api.oozie.DeployCommand(coordinatorName = cname, id = id, coordinator = coordinator, actions = actions, status = status, errors = List())
		}.map(cmd => validate(cmd, done))
		api.oozie.DeployInfo(commands = commands.toSeq, done = done, autoDeploy = autoDeploy, errors = commands.map(_.errors).toList.flatten, oldErrors = errs)
	}

	def parseDeployInfo(stateConf: Config, deployConf: Config, project: api.oozie.Project): api.oozie.DeployInfo = {
		case class IdStatus(id: Option[api.oozie.Coordinator.Id], status: Option[String]) 
		// TODO: Use enums for commands
		val done = parseDoneFlag(deployConf)
		val autoDeploy = parseAutoDeploy(deployConf)
		val errs = parseErrors(deployConf)
		val (sconf: Config, dconf: Config) = try {
			(stateConf.getConfig("state.oozie"), deployConf.getConfig("deploy.oozie.coordinators"))
		} catch {
			case e: Exception =>
				return api.oozie.DeployInfo(done = done, autoDeploy = autoDeploy, errors = List(api.oozie.DeployError.from(e)), oldErrors = errs)
		}
		val cmap = project.coordinators.map{ c => (c.name, c) }.toMap
		val imap = sconf.entrySet.asScala.map{ opt =>
			val cname = opt.getKey
			val st = sconf.getString(cname).split('|')
			(cname, IdStatus(id = Some(st.last), Some(st.head)))
		}.toMap
		val commands = dconf.entrySet.asScala.map{ item =>
			val cname = item.getKey
			val actions = dconf.getString(cname).split(',').map(_.trim).toList
			val id = imap.get(cname).getOrElse(IdStatus(None, None)).id
			val status = imap.get(cname).getOrElse(IdStatus(None, None)).status
			val coordinator = cmap.get(cname)
			api.oozie.DeployCommand(coordinatorName = cname, id = id, coordinator = coordinator, actions = actions, status = status, errors = List())
		}.map(cmd => validate(cmd, done))

		val usedCoordNames = commands.map(_.coordinatorName).toSet
		val virtualCommands = sconf.entrySet.asScala.map{ opt =>
			val cname = opt.getKey
			val st = sconf.getString(cname).split('|')
			val status = st.head
			val id = st.last
			(cname, status, id)
		}.filter(t => !usedCoordNames.contains(t._1)).map{ t =>
			api.oozie.DeployCommand(coordinatorName = t._1, id = Some(t._3), coordinator = cmap.get(t._1), actions = List("nop"), status = Some(t._2))
		}

		api.oozie.DeployInfo(commands = commands.toSeq ++ virtualCommands.toSeq, done = done, autoDeploy = autoDeploy, errors = commands.map(_.errors).toList.flatten, oldErrors = errs)
	}

	def unparseDeployInfo(deploy: api.oozie.DeployInfo): (Config, Config) = {
		val state = deploy.commands.filter(!_.id.isEmpty).map{ cmd =>
			(cmd.coordinatorName, ConfigValueFactory.fromAnyRef(s"${cmd.status.get}|${cmd.id.get}"))
		}.toMap.asJava
		val commands = deploy.commands.map{ cmd =>
			(cmd.coordinatorName, ConfigValueFactory.fromAnyRef(cmd.actions.mkString(",")))
		}.toMap.asJava
		val sconf = ConfigFactory.empty.withValue("state.oozie", ConfigValueFactory.fromAnyRef(state))
		val dconf = ConfigFactory
			.empty
			.withValue("deploy.oozie.coordinators", ConfigValueFactory.fromAnyRef(commands))
			.withValue("deploy.oozie.done", ConfigValueFactory.fromAnyRef(deploy.done))
			.withValue("deploy.oozie.errors", ConfigValueFactory.fromIterable(deploy.errors.map(_.message).asJava))
		(sconf, dconf)
	}

	def deploy2conf(deploy: api.oozie.DeployInfo): Config = {
		val commands = deploy.commands.map{ cmd =>
			(cmd.coordinatorName, ConfigValueFactory.fromAnyRef(cmd.actions.mkString(",")))
		}.toMap.asJava
		val dconf = ConfigFactory
			.empty
			.withValue("deploy.oozie.coordinators", ConfigValueFactory.fromAnyRef(commands))
			.withValue("deploy.oozie.done", ConfigValueFactory.fromAnyRef(deploy.done))
			.withValue("deploy.oozie.autodeploy", ConfigValueFactory.fromAnyRef(deploy.autoDeploy.name))
			.withValue("deploy.oozie.errors", ConfigValueFactory.fromIterable(deploy.errors.map(_.message).asJava))
		dconf
	}


	def emptyStateConf: Config = {
		// Empty state:
		// state: { oozie: {} }
		ConfigFactory.empty.withValue(
			"state", ConfigFactory
				.empty
				.withValue("oozie", emptyMap)
				.root
		)
	}

	def emptyDeployConf: Config = {
		// Empty deploy:
		// deploy: { oozie: {}, done: true|false, errors: [] }
		ConfigFactory.empty.withValue(
			"deploy", ConfigFactory
				.empty
				.withValue("oozie", ConfigFactory.empty.withValue("coordinators", emptyMap).root)
				.withValue("done", ConfigValueFactory.fromAnyRef(true))
				.withValue("errors", emptyList)
				.root
		)
	}

	private def parseDoneFlag(conf: Config): Boolean = {
		try {
			conf.getBoolean("deploy.oozie.done")
		} catch {
			case e: ConfigException => false
		}
	}

	private def parseAutoDeploy(conf: Config): api.Profile = {
		try {
			api.Profile(name = conf.getString("deploy.oozie.autodeploy"), desc = "")
		} catch {
			case e: ConfigException => api.Profile(name = "", desc = "")
		}
	}

	private def parseErrors(conf: Config): List[api.oozie.DeployError] = {
		try {
			conf.getStringList("deploy.oozie.errors").asScala.map(api.oozie.DeployError.from).toList
		} catch {
			case e: ConfigException => List()
		}
	}

	private def validate(cmd: api.oozie.DeployCommand, done: Boolean): api.oozie.DeployCommand = {
		if (done) { return cmd }
		if (cmd.actions.contains("start") && cmd.coordinator.isEmpty) {
			cmd.withError(s"Coordinator ${cmd.coordinatorName} not found in project")
		} else {
			cmd
		}
	}

	private def emptyMap: ConfigValue = ConfigValueFactory.fromMap(Map.empty[String, Object].asJava)
	private def emptyList: ConfigValue = ConfigValueFactory.fromIterable(List.empty[String].asJava)

}
