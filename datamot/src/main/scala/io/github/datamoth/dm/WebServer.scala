package io.github.datamoth.dm

import scala.util.Try
import scala.util.Failure

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory

import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.pattern.{ ask }
import akka.stream.ActorMaterializer
import akka.util.Timeout

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.LoggingMagnet
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.model.headers.HttpOriginRange.*
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.RouteResult

import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink

import com.softwaremill.session.{SessionConfig, SessionManager}
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session.BasicSessionEncoder
import com.softwaremill.session.SessionSerializer
import com.softwaremill.session.MultiValueSessionSerializer

import de.heikoseeberger.akkahttpjackson.JacksonSupport
import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.CorsSettings

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration._

import java.io.IOException


import scalaz._
import Scalaz._


case class Result(code: Int, msg: String, data: Any)
object Result {
	val SUCCESS = 0
	val FAILURE = 1
	def ok: Result = Result(SUCCESS, "ok", "ok")
	def ok(data: Any): Result = Result(SUCCESS, "ok", data)
	def oops(data: Any): Result = Result(FAILURE, "oops", data)
	def oops(msg: String): Result = Result(FAILURE, msg, Array())
	def oops(e: Exception): Result = Result(FAILURE, e.getMessage, Array())
}

class WebServer extends Directives with JacksonSupport {

	import io.github.datamoth.dm.Worker

	case class PikaState(user: String)

	object PikaState {
		private val USER = "user"
		implicit def serializer: MultiValueSessionSerializer[PikaState] = {
			new MultiValueSessionSerializer[PikaState](
				toMap = { state: PikaState =>
					Map(
						USER -> state.user
					)
				},
				fromMap = { m: Map[String, String] =>
					if (m.contains(USER)) {
						Try(PikaState(
							user = m(USER)
						))
					} else {
						Failure[PikaState](new Exception("Failed to parse session"))
					}
				}
			)
		}
	}

	val sessionConfig = SessionConfig.default("please_refactor_me________________________________________________________________")

	implicit val sessionSerializer = new MultiValueSessionSerializer[Map[String, String]](identity, Try(_))
	implicit val sessionEncoder = new BasicSessionEncoder[PikaState]
	implicit val sessionManager = new SessionManager[PikaState](sessionConfig)

	private val L = LoggerFactory.getLogger(classOf[WebServer])

	def getConfig(): Config = {
		try {
			ConfigFactory.load.withValue("admin.origin", ConfigValueFactory.fromAnyRef(sys.env("DATAMOT_ADMIN_REPO_DIR")))
		} catch {
			case e: Exception => ConfigFactory.load
		}
	}

	def start(host: String, port: Int): Unit = {

		import HttpCharsets._
		import Http.IncomingConnection

		implicit val timeout = Timeout(60 seconds)
		implicit val system = ActorSystem("pikazu")
		implicit val materializer = ActorMaterializer()
		implicit val executionContext = system.dispatcher


		val rand = scala.util.Random
		def gentoken = rand.alphanumeric.take(10).mkString
		val root = system.actorOf(Props[Worker.Root], "root")

		implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
			case _: IOException => extractUri { uri =>
				L.error("Unknown exception occured for {}", uri)
				complete(Result.oops("Unknown exception"))
			}
		}

		val corsSettings = CorsSettings.defaultSettings.copy(allowedOrigins = *)

		val route = cors() {
			extractClientIP { ip =>
				L.info("client ip: {}", ip)
				handleExceptions(exceptionHandler) {
					path("app.js") { getFromResource("app.js") } ~
					path("enterthematrix") { get {
						parameters("user") { (user) =>
							setSession(oneOff, usingCookies, PikaState(user)) { ctx =>
								ctx.complete{ Result.ok(user) }
							}
						}
					} } ~
					path("leavethematrix") { get {
						invalidateSession(oneOff, usingCookies) {
							complete{ Result.ok }
						}
					} } ~
					path("whoami") { get {
						optionalSession(oneOff, usingCookies) { stateOpt => stateOpt match {
							case None => complete{ Result.oops("Not authenticated") }
							case Some(state) => complete{ Result.ok(state.user) }
						} }
					} } ~
					pathPrefix("apiv1") {
						val token = "datamotapi"
						path(Segments) { sg => { parameterSeq { params => {
							val pars = params.groupBy(_._1).map{ case (k, v) => k -> v.map(_._2).toList }
							val args = pars |+| List("namespace", "project", "ref", "profile").zip(sg.map(List(_))).toMap
							val msg = (token, Worker.Req(args = args))
							onSuccess(root ? msg) { resp =>
								resp match {
									case r: Worker.Res => complete{ Result.ok(resp) }
									case r: Worker.Fail => complete{ Result.oops(resp) }
								}
							}
						} } } }
					} ~
					pathPrefix("api") {
						requiredSession(oneOff, usingCookies) { state =>
							val token = state.user
							path(Segments) { sg => { parameterSeq { params => {
								val pars = params.groupBy(_._1).map{ case (k, v) => k -> v.map(_._2).toList }
								val args = pars |+| List("namespace", "project", "ref", "profile").zip(sg.map(List(_))).toMap
								val msg = (token, Worker.Req(args = args))
								onSuccess(root ? msg) { resp =>
									resp match {
										case r: Worker.Res => complete{ Result.ok(resp) }
										case r: Worker.Fail => complete{ Result.oops(resp) }
									}
								}
							} } } }
						}
					} ~
					path("") { get {
						getFromResource("index.html")
					} }
				}
			}
		}

		def logreq(req: HttpRequest): Unit = L.debug("REQ: {}", req)
		def logres(res: RouteResult): Unit = L.debug("RES: {}", res)

		val routeReqLogged = DebuggingDirectives.logRequest(LoggingMagnet(_ => logreq))(route)
		val routeResLogged = DebuggingDirectives.logRequest(LoggingMagnet(_ => logreq))(route)
		val routeReqResLogged = DebuggingDirectives.logResult(LoggingMagnet(_ => logres))(routeReqLogged)

		val routeLogged = route

		def handler(req: HttpRequest): Future[HttpResponse] = {
			val innerHandler = Route.asyncHandler(routeLogged)
			val f = innerHandler(req)
			f
		}

		val binding = Http().bind(host, port)

		binding
		.to(
			Sink.foreach { connection =>
				connection.handleWithAsyncHandler(handler)
			}
		)
		.run()

	}

}
