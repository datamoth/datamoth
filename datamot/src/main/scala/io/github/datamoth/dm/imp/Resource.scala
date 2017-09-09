package io.github.datamoth.dm.imp


object AsResource {
	class AutoCloseableWrapper[A <: AutoCloseable](protected val c: A) {
		def map[B](f: (A) => B): B = {
			try {
				f(c)
			} finally {
				c.close()
			}
		}
		def foreach(f: (A) => Unit): Unit = map(f)
		def flatMap[B](f: (A) => B): B = map(f)
		def withFilter(f: (A) => Boolean) = this
	}
	def apply[A <: AutoCloseable](c: A) = new AutoCloseableWrapper(c)
}
