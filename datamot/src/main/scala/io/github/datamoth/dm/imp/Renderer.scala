package io.github.datamoth.dm.imp

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File
import java.io.FileWriter
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.DefaultMustacheVisitor
import com.github.mustachejava.Mustache
import com.github.mustachejava.MustacheException
import com.github.mustachejava.MustacheVisitor
import com.github.mustachejava.TemplateContext
import com.github.mustachejava.codes.ExtendNameCode
import com.github.mustachejava.codes.ValueCode
import com.github.mustachejava.reflect.MissingWrapper
import com.github.mustachejava.reflect.ReflectionObjectHandler
import com.github.mustachejava.util.Wrapper

import scala.collection.JavaConverters._

import io.github.datamoth.dm.api


object Renderer {


	case class Result(errors: List[api.Error], plugins: List[api.Plugin], files: List[api.Location])

	case class Config(sysvars: com.typesafe.config.Config)

	def create(sysvars: com.typesafe.config.Config): Renderer = {
		new Renderer(Config(sysvars))
	}
}

class Renderer(cfg: Renderer.Config) {

	import scala.collection.mutable.HashMap
	import scala.collection.mutable.ArrayBuffer

	private val L = LoggerFactory.getLogger(classOf[Renderer])

	private type Scope = java.util.Map[String, Object]

	def render(srcDir: File, dstDir: File, profile: String): Renderer.Result = {
		L.debug("Render: profile {}, from {} into {}", profile, srcDir, dstDir)
		errors.clear()
		renderTree(profile, srcDir, dstDir)
		return Renderer.Result(errors = errors.toList, plugins = plugins.toList, files = files.toList)
	}

	private def renderTree(profile: String, srcDir: File, dstDir: File): Unit = {
		val srcRoot = srcDir
		val dstRoot = dstDir
		def walk(srcDir: File, dstDir: File, parentCfg: Config, prof: String, tr: (File, File, Scope) => Unit): Unit = {
			def transform = (f: File, scope: Scope) => {
				val relPath = srcDir.toURI.relativize(f.toURI).getPath
				val srcFile = srcDir.toPath.resolve(relPath).toFile
				val dstFile = dstDir.toPath.resolve(relPath).toFile
				val curPath = new File(dstRoot.toURI.relativize(dstFile.toURI).getPath).getParentFile

				scope.put("CURRENT_DIR", if (curPath == null) "" else curPath.getName)
				scope.put("CURRENT_PATH", new File(scope.get("PROJECT_DIR").toString, if (curPath == null) "" else curPath.toString).toString)

				tr(srcFile, dstFile, scope)
			}
			def walk(dir: File, parentCfg: Config): Unit = {
				if (dir.getName.startsWith(".")) return
				val cfgPath = new File(dir, ".conf")
				var cnf = parentCfg
				if (cfgPath.exists) {
					L.debug("Try to load config: {}", cfgPath)
					cnf = ConfigFactory.parseFile(cfgPath).withFallback(parentCfg).resolve
					if (cnf.hasPath(prof)) {
						cnf = cnf.getConfig(prof).withFallback(parentCfg)
					}
					cnf = cnf.resolve
				}
				dir.listFiles.filter(_.isDirectory).map(walk(_, cnf))
				dir.listFiles.filter(_.isFile).map(transform(_, cnf.root().unwrapped))
			}
			walk(srcDir, parentCfg)
		}
		walk(srcDir, dstDir, cfg.sysvars, profile, (srcFile: File, dstFile: File, scope: Scope) => {
			val name = srcDir.toURI.relativize(srcFile.toURI).getPath
			if (name.endsWith(".plug") || name.endsWith(".plug")) {
				val plugName = srcFile.getName.split("\\.(?=[^\\.]+$)")(0)
				L.debug("Render plugin: {}", srcFile)
				val content = new String(Files.readAllBytes(srcFile.toPath), StandardCharsets.UTF_8)
				val conf = ConfigFactory.parseFile(srcFile).withFallback(ConfigFactory.parseMap(scope)).resolve
				val pdir = new File(srcDir, conf.getString("run"))
				val plugConf = conf.getConfig(s"with.${profile}").root.unwrapped

				plugins += api.Plugin(location = api.Location(name), run = api.Location(conf.getString("run")), conf = content)
				walk(pdir, dstFile.getParentFile, cfg.sysvars, profile, (srcFile: File, dstFile: File, sc: Scope) => {
					scope.asScala.keys.foreach{ k => if (!sc.containsKey(k)) { sc.put(k, scope.get(k)) } }
					plugConf.asScala.keys.foreach{ k => sc.put(k, plugConf.get(k)) }
					sc.put("PLUGIN_INSTANCE_PATH", name.split("\\.(?=[^\\.]+$)")(0))
					sc.put("PLUGIN_INSTANCE_NAME", plugName)
					renderFile(srcFile, dstFile, name, sc)
				})
			} else {
				renderFile(srcFile, dstFile, name, scope)
			}
		})
	}

	private def rndstr(len: Int): String = scala.util.Random.alphanumeric.take(len).mkString

	private def renderFile(srcFile: File, dstFile: File, name: String, scope: Scope): Unit = {
		dstFile.getParentFile.mkdirs()
		for (
			out <- AsResource(new FileWriter(dstFile));
			tml <- AsResource(new FileReader(srcFile))
		) {
			val muf = new DefaultMustacheFactory {
				override def encode(value: String, writer: java.io.Writer): Unit = {
					value match {
						case "_rnd_alnum_5_"	=> writer.write(rndstr(5))
						case "_rnd_alnum_10_"	=> writer.write(rndstr(10))
						case "_rnd_alnum_15_"	=> writer.write(rndstr(15))
						case "_rnd_alnum_20_"	=> writer.write(rndstr(20))
						case _					=> writer.write(value)
					}
				}
				override def createMustacheVisitor(): MustacheVisitor = new DefaultMustacheVisitor(this) {
					override def value(tc: TemplateContext, path: String, encoded: Boolean): Unit = {
						if (!contains(path, scope)) {
							// TODO: Systematize error codes and kinds
							errors += api.VarNotFoundError(
								location = Some(api.Location(tc.file, Some(tc.line), None))
								, code = -1
								, kind = "render error"
								, message = s"Variable not found: ${path}"
							)
						}
						list.add(new ValueCode(tc, df, path, encoded))
					}
					override def name(tc: TemplateContext, path: String, mustache: Mustache): Unit = {
						list.add(new ExtendNameCode(tc, df, mustache, path));
					}
				}
			}
			// muf.setObjectHandler(new ReflectionObjectHandler{})
			val mus = muf.compile(tml, name)
			try {
				mus.execute(out, scope)
				files += api.Location(file = name)
			} catch {
				case e: MustacheException =>
					errors += api.RenderError(
						location = Some(api.Location(name, None, None))
						, code = -1
						, kind = "render error"
						, message = e.getMessage
					)
			}
			out.flush
		}
	}

	private val errors = ArrayBuffer[api.Error]()
	private val plugins = ArrayBuffer[api.Plugin]()
	private val files = ArrayBuffer[api.Location]()

	private def contains(path: String, map: java.util.Map[String, Object]): Boolean = {
		val items = path.split(".")
		var next = map
		for (item <- items) {
			next = next.get(item).asInstanceOf[java.util.HashMap[String, Object]]
			if (next == null) {
				return false
			}
		}
		return true
	}
}
