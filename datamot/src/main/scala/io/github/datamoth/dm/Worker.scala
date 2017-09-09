package io.github.datamoth.dm

import akka.actor.Props
import akka.actor.Actor
import akka.actor.Terminated
import akka.actor.ActorRef
import akka.actor.ActorLogging
import akka.actor.ActorSystem

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import scala.collection.JavaConverters._

import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.concurrent._
import scala.concurrent.duration._

import org.slf4j.Logger
import org.slf4j.LoggerFactory


object Worker {

	import io.github.datamoth.dm.api
	import java.time.LocalDateTime

	type DT = LocalDateTime
	type Key = (String, String, String, String)

	type Ok[+A, +B] = Left[A, B]
	type No[+A, +B] = Right[A, B]

	val Ok = Left
	val No = Right

	case class Req(args: Map[String, List[String]]) {
		def has(a: String): Boolean = args.contains(a) && args(a).size > 0
		def gets(k: String): String = args(k).head
		def getl(k: String): List[String] = args(k)
		def get(a1: String): Either[String, String] = {
			if (!has(a1)) return No(s"$a1 required")
			Ok(gets(a1))
		}
		def get(a1: String, a2: String): Either[(String, String), String] = {
			if (!has(a1)) return No(s"$a1 required")
			if (!has(a2)) return No(s"$a2 required")
			Ok(gets(a1), gets(a2))
		}
		def get(a1: String, a2: String, a3: String): Either[(String, String, String), String] = {
			if (!has(a1)) return No(s"$a1 required")
			if (!has(a2)) return No(s"$a2 required")
			if (!has(a3)) return No(s"$a3 required")
			Ok(gets(a1), gets(a2), gets(a3))
		}
		def get(a1: String, a2: String, a3: String, a4: String): Either[(String, String, String, String), String] = {
			if (!has(a1)) return No(s"$a1 required")
			if (!has(a2)) return No(s"$a2 required")
			if (!has(a3)) return No(s"$a3 required")
			if (!has(a4)) return No(s"$a4 required")
			Ok(gets(a1), gets(a2), gets(a3), gets(a4))
		}
	}

	case class Res(req: Req, data: AnyRef)
	case class Fail(req: Req, data: String)

	class Root extends Actor {

		private val L = LoggerFactory.getLogger(classOf[Root])

		private def workerOf(token: String): ActorRef = {
			context.child(token).getOrElse(context.actorOf(Props[Work], token))
		}

		override def receive = {
			case (token: String, req: Req) => {
				val who = sender
				val wkr = workerOf(token)
				wkr ! (who, req)
			}
		}

	}

	class Work extends Actor with ActorLogging {

		import context.dispatcher

		private val L = LoggerFactory.getLogger(classOf[Work])

		private val token = self.path.name
		private val datamot = Datamot.create(token)

		override def preStart() = {
			L.debug("Worker starting for {}", token)
		}

		override def receive = {
			case (who: ActorRef, req: Req) => {
				req.get("action") match {
					case Ok(act) =>	act match {
						case "list-projects"	=> who ! Res(req = req, data = datamot.projects)
						case "list-namespaces"	=> who ! Res(req = req, data = datamot.namespaces)
						case _ => req.get("namespace", "project") match {
							case Ok((ns, pr)) => act match {
								case "list-refs" => who ! Res(req = req, data = datamot.getRefs(ns, pr))
								case _ => req.get("ref") match {
									case Ok(rf) => act match {
										case "list-profiles" => who ! Res(req = req, data = datamot.getProfiles(ns, pr, rf))
										case "list-commits" => who ! Res(req = req, data = datamot.getCommitLog(ns, pr, rf))
										case _ => req.get("profile") match {
											case Ok(pf) => act match {
												case "list-datasets" => who ! Res(req = req, data = datamot.getProject(ns, pr, rf, pf).datasets)
												case "get-project" => who ! Res(req = req, data = datamot.getProject(ns, pr, rf, pf))
												case "get-files" => {
													if (!req.has("paths")) {
														who ! Fail(req = req, data = "paths required")
													} else {
														val paths = req.getl("paths")
														who ! Res(req = req, data = datamot.getFiles(ns, pr, rf, pf, paths))
													}
												}
												case "get-oozie-coordinator-info" => {
													if (!req.has("name")) {
														who ! Fail(req = req, data = "name required")
													} else {
														val name = req.gets("name")
														who ! Res(req = req, data = datamot.getOozieCoordinatorInfo(ns, pr, rf, pf, name))
													}
												}
												case "get-oozie-jobs" => {
													if (!req.has("max-count")) {
														who ! Res(req = req, data = datamot.getOozieJobs(ns, pr, rf, pf, 3000))
													} else {
														val maxCountResult = Try(Integer.parseInt(req.gets("max-count")))
														maxCountResult match {
															case Success(maxCount) =>
																who ! Res(req = req, data = datamot.getOozieJobs(ns, pr, rf, pf, maxCount))
															case Failure(msg) =>
																who ! Fail(req = req, data = "<max-count> should be an integer")
														}
													}
												}
												case "hdfs-ls" => {
													if (!req.has("path")) {
														who ! Fail(req = req, data = "path required")
													} else {
														val path = req.gets("path")
														who ! Res(req = req, data = datamot.hdfsLs(ns, pr, rf, pf, path))
													}
												}
												case _ => who ! Fail(req = req, data = s"Unknown method ${act}")
											}
											case No(msg) => who ! Fail(req = req, data = msg)
										}
									}
									case No(msg) => who ! Fail(req = req, data = msg)
								}
							}
							case No(msg) => who ! Fail(req = req, data = msg)
						}
					}
					case No(msg) => who ! Fail(req = req, data = msg)
				}
			}
		}

	}

}
