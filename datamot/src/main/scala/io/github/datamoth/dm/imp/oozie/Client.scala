package io.github.datamoth.dm.imp.oozie

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File
import java.nio.file.Files
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils

import scala.collection.JavaConverters._


object Client {

	case class Config(
		user: String
		, hdfsDir: String
		, sysopts: com.typesafe.config.Config
	)

	def create(user: String, hdfsDir: String, sysopts: com.typesafe.config.Config): Client = {
		new Client(Config(
			user = user
			, hdfsDir = hdfsDir
			, sysopts = sysopts
		))
	}

}

class Client(cfg: Client.Config) {

	import io.github.datamoth.dm.api
	import scala.collection.JavaConverters._
	import org.apache.oozie.client.OozieClient
	import org.apache.oozie.client.WorkflowJob
	import org.apache.oozie.client.CoordinatorJob
	import org.apache.oozie.client.OozieClientException

	private val L = LoggerFactory.getLogger(classOf[Client])
	private val client = new OozieClient(cfg.sysopts.getString("uri"))
	private val ALL_JOBS = "STATUS=PREP;STATUS=RUNNING;STATUS=SUCCEEDED;STATUS=KILLED;STATUS=FAILED;STATUS=SUSPENDED"
	private val ALL_ALIVE_COORDS = "STATUS=IGNORED;STATUS=PAUSED;STATUS=PAUSEDWITHERROR;STATUS=PREMATER;STATUS=PREP;STATUS=PREPPAUSED;STATUS=PREPSUSPENDED;STATUS=RUNNING;STATUS=RUNNINGWITHERROR;STATUS=SUSPENDED;STATUS=SUSPENDEDWITHERROR"
	private val ALL_ACTIONS = "STATUS=FAILED;STATUS=IGNORED;STATUS=KILLED;STATUS=READY;STATUS=RUNNING;STATUS=SKIPPED;STATUS=SUBMITTED;STATUS=SUCCEEDED;STATUS=SUSPENDED;STATUS=TIMEDOUT;STATUS=WAITING"

	def getJobs(maxCount: Int): List[WorkflowJob] = {
		L.debug("Try to get jobs list")
		val jobs = client.getJobsInfo(ALL_JOBS, 0, maxCount).asScala
		jobs.toList
	}

	def getCoordinatorInfo(name: String): List[AnyRef] = {
		client.getCoordJobsInfo(ALL_ALIVE_COORDS + s";NAME=${name}", 0, 100).asScala.map{ c =>
			client.getCoordJobInfo(c.getId, ALL_ACTIONS, 0, 30, "desc")
		}.toList
	}

	def deploy(deploy: api.oozie.DeployInfo): api.oozie.DeployInfo = {
		val result = deploy.commands.map { cmd =>
			if (cmd.errors.isEmpty) {
				cmd.actions.foldLeft(cmd) { case (c, action) =>
					action match {
						case "start"		=> start(c)
						case "kill"			=> kill(c)
						case "resume"		=> resume(c)
						case "suspend"		=> suspend(c)
						case _				=> c
					}
				}
			} else {
				cmd
			}
		}
		deploy.setCommands(commands = result).withCommandErrors
	}

	private def start(cmd: api.oozie.DeployCommand): api.oozie.DeployCommand = {
		val conf = client.createConfiguration()
		try {
			val c = cmd.coordinator.get
			for (opt <- cfg.sysopts.entrySet.asScala) {
				L.debug("Set oozie sysopt {} <- {}", opt.getKey:Any, cfg.sysopts.getString(opt.getKey))
				conf.setProperty(opt.getKey, cfg.sysopts.getString(opt.getKey))
			}
			conf.setProperty("oozie.coord.application.path", new File(new File(cfg.hdfsDir), c.location.file).toString)
			L.info("Starting: {}", c.name)
			val id = client.run(conf)
			L.info("Started: {}, id {}", c.name:Any, id)
			cmd.withId(id).withStatus("running")
		} catch {
			case e: OozieClientException =>
				L.error("Oozie client error [while starting]: {}", e.getMessage)
				cmd.withoutId.withError(s"${e.getErrorCode}:${e.getMessage}")
			case e: Exception =>
				L.error("Oozie client error [while starting]: {}", e.getMessage)
				cmd.withoutId.withError(e.getMessage)
		}
	}

	private def kill(cmd: api.oozie.DeployCommand): api.oozie.DeployCommand = {
		try {
			val list = getCoordsByName(cmd.coordinatorName)
			list.foreach { c =>
				L.info("Killing: {}:{}", c.getAppName:Any, c.getId)
				client.kill(c.getId)
				L.info("Killed: {}:{}", c.getAppName:Any, c.getId)
			}
			cmd.withoutId.withoutStatus
		} catch {
			case e: OozieClientException =>
				L.error("Oozie client error [while killing], {}", e.getMessage)
				cmd.withError(s"${e.getErrorCode}:${e.getMessage}")
			case e: Exception =>
				L.error("Oozie client error [while killing], {}", e.getMessage)
				cmd.withError(e.getMessage)
		}
	}

	private def suspend(cmd: api.oozie.DeployCommand): api.oozie.DeployCommand = {
		try {
			val list = getCoordsByName(cmd.coordinatorName)
			list.foreach { c =>
				L.info("Suspending: {}:{}", c.getAppName:Any, c.getId)
				client.suspend(c.getId)
				L.info("Suspended: {}:{}", c.getAppName:Any, c.getId)
			}
			cmd.withStatus("suspended")
		} catch {
			case e: OozieClientException =>
				L.error("Oozie client error [while suspending]: {}", e.getMessage)
				cmd.withError(s"${e.getErrorCode}:${e.getMessage}")
			case e: Exception =>
				L.error("Oozie client error [while suspending]: {}", e.getMessage)
				cmd.withError(e.getMessage)
		}
	}

	private def resume(cmd: api.oozie.DeployCommand): api.oozie.DeployCommand = {
		try {
			val list = getCoordsByName(cmd.coordinatorName)
			list.foreach { c =>
				L.info("Resuming: {}:{}", c.getAppName:Any, c.getId)
				client.suspend(c.getId)
				L.info("Resumed: {}:{}", c.getAppName:Any, c.getId)
				cmd.withoutId.withoutStatus
			}
			cmd.withStatus("running")
		} catch {
			case e: OozieClientException =>
				L.error("Oozie client error [while resuming]: {}", e.getMessage)
				cmd.withError(s"${e.getErrorCode}:${e.getMessage}")
			case e: Exception =>
				L.error("Oozie client error [while resuming]: {}", e.getMessage)
				cmd.withError(e.getMessage)
		}
	}

	private def getCoordsByName(name: String): List[CoordinatorJob] = {
		val rawList = client.getCoordJobsInfo(ALL_ALIVE_COORDS + s";NAME=${name}", 0, 1000)
		val map = rawList.asScala.groupBy(_.getAppName)
		val list = map.get(name).getOrElse(List[CoordinatorJob]())
		if (list.length == 0) {
			throw new java.util.NoSuchElementException(s"Coordinator ${name} not found")
		}
		list.toList
	}

}
