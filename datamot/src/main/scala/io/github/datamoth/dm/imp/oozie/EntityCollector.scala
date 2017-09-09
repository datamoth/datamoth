package io.github.datamoth.dm.imp
package oozie

import java.io.File
import io.github.datamoth.dm.api


class EntityCollector {

	import scala.collection.mutable.ArrayBuffer

	private val clist = new ArrayBuffer[api.oozie.Coordinator]
	private val dlist = new ArrayBuffer[api.oozie.Dataset]
	private val blist = new ArrayBuffer[api.oozie.Databundle]
	private val wlist = new ArrayBuffer[api.oozie.Workflow]
	private val elist = new ArrayBuffer[api.Error]
	private val warns = new ArrayBuffer[api.Error]
	private val plist = new ArrayBuffer[api.Plugin]
	private val flist = new ArrayBuffer[api.Location]
	private val gugo = new Gugo

	def collectCoordinator(c: api.oozie.Coordinator): Unit = {
		clist.append(c)
	}

	def collectWorkflow(w: api.oozie.Workflow): Unit = {
		wlist.append(w)
	}

	def collectDatabundle(b: api.oozie.Databundle): Unit = {
		blist.append(b)
	}

	def collectDataset(d: api.oozie.Dataset): Unit = {
		dlist.append(d)
	}

	def collectError(e: api.Error): Unit = {
		elist.append(e)
	}

	def collectWarning(e: api.Error): Unit = {
		warns.append(e)
	}

	def collectPlugin(p: api.Plugin): Unit = {
		plist.append(p)
	}

	def collectFile(f: api.Location): Unit = {
		flist.append(f)
		gugo.add(f.file)
	}

	def coordinators		= clist.toSeq.toList
	def databundles			= blist.toSeq.toList
	def workflows			= wlist.toSeq.toList
	def datasets			= dlist.toSeq.toList
	def errors				= elist.toSeq.toList
	def warnings			= warns.toSeq.toList
	def plugins				= plist.toSeq.toList
	def files				= flist.toSeq.toList


	def bestPath(path: String): String = gugo.best(path)

	private class Gugo extends scala.collection.mutable.HashMap[String, Gugo] {
		def add(argpaths: String*): Unit = argpaths.foreach(add)
		def add(argpath: String): Unit = {
			val path = argpath.split(File.separator).reverse
			path.foldLeft(this) { case (r, item) =>
				if (r.contains(item)) r(item)
				else {
					val g = new Gugo
					r.put(item, g)
					g
				}
			}
		}
		def best(argpath: String): String = {
			import scala.collection.mutable.ArrayBuffer
			val path = argpath.split(File.separator).reverse.toList
			def next(tail: List[String], gugo: Gugo, res: ArrayBuffer[String]): ArrayBuffer[String] = tail match {
				case Nil => res
				case item :: tail =>
					if (gugo.contains(item)) {
						res += item
						next(tail, gugo(item), res)
					} else res
			}
			next(path, this, ArrayBuffer.empty[String]).toList.reverse.mkString(File.separator)
		}
	}

}
