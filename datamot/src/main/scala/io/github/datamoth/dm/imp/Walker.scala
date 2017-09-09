package io.github.datamoth.dm.imp


import java.io.File


object Walker {

	def walk[T](root: String, filter: (File) => Boolean, visit: (File) => T): Unit = {
		walk(new File(root), filter, visit)
	}

	def walk[T](root: String, visit: (File) => T): Unit = {
		walk(new File(root), visit)
	}

	def walk[T](root: File, visit: (File) => T): Unit = {
		def relPathVisitor(rt: File, path: File) {
			val relPath = root.toURI.relativize(path.toURI).getPath
			visit(new File(relPath))
		}
		def next(dir: File): Unit = {
			val err = new java.nio.file.NoSuchFileException(dir.toString)
			Option(dir.listFiles).getOrElse(throw err).filter(_.isDirectory).map(next)
			Option(dir.listFiles).getOrElse(throw err).filter(_.isFile).map(relPathVisitor(root, _))
		}
		next(root)
	}

	def walk[T](root: File, filter: (File) => Boolean, visit: (File) => T): Unit = {
		def relPathVisitor(rt: File, path: File) {
			val relPath = root.toURI.relativize(path.toURI).getPath
			visit(new File(relPath))
		}
		def next(dir: File): Unit = {
			val err = new java.nio.file.NoSuchFileException(dir.toString)
			Option(dir.listFiles).getOrElse(throw err).filter(_.isDirectory).filter(filter).map(next)
			Option(dir.listFiles).getOrElse(throw err).filter(_.isFile).filter(filter).map(relPathVisitor(root, _))
		}
		next(root)
	}

}
