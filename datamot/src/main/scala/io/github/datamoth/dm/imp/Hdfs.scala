package io.github.datamoth.dm.imp

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File
import java.security.PrivilegedExceptionAction

import org.apache.hadoop.fs.Path
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.FileStatus
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation

import scala.collection.JavaConverters._


object Hdfs {

	case class Item(name: String, path: String, isDir: Boolean, modified: Long, size: Long, space: Long)

	case class Config(user: String, sysopts: com.typesafe.config.Config)

	def create(user: String, sysopts: com.typesafe.config.Config): Hdfs = {
		new Hdfs(Config(user, sysopts))
	}

}

class Hdfs(cfg: Hdfs.Config) {

	private val L = LoggerFactory.getLogger(classOf[Hdfs])
	private val hdfsConf = new Configuration

	{
		for (opt <- cfg.sysopts.entrySet.asScala) {
			L.debug("Set hdfs sysopt {} <- {}", opt.getKey:Any, cfg.sysopts.getString(opt.getKey))
			hdfsConf.set(opt.getKey, cfg.sysopts.getString(opt.getKey))
		}
	}

	def ls(path: String): List[AnyRef] = {
		L.info("User: {}, ls path {}", cfg.user:Any, path)
		val ugi = UserGroupInformation.createRemoteUser(cfg.user);
		var items = scala.collection.mutable.ArrayBuffer.empty[Hdfs.Item]
		ugi.doAs(new PrivilegedExceptionAction[Void]() {
			override def run(): Void = {
				for (fs <- AsResource(FileSystem.get(hdfsConf))) {
					fs.listStatus(new Path(path)).sortBy(_.getModificationTime).reverse.foreach{ item =>
						val summary = fs.getContentSummary(item.getPath)
						items += Hdfs.Item(
							name = item.getPath.getName
							, path = Path.getPathWithoutSchemeAndAuthority(item.getPath).toString
							, isDir = item.isDir
							, modified = item.getModificationTime
							, size = summary.getLength
							, space = summary.getSpaceConsumed
						)
					}
				}
				return null
			}
		})
		items.toList
	}

	def upload(srcDir: File, dstDir: String): Unit = {
		L.info("User: {}, Upload {} -> {}", cfg.user, srcDir, dstDir)
		val ugi = UserGroupInformation.createRemoteUser(cfg.user);
		ugi.doAs(new PrivilegedExceptionAction[Void]() {
			override def run(): Void = {
				for (fs <- AsResource(FileSystem.get(hdfsConf))) {
					fs.delete(new Path(dstDir), true)
					fs.copyFromLocalFile(new Path(srcDir.getAbsolutePath), new Path(dstDir))
				}
				return null
			}
		})
	}

}
