package io.github.datamoth.dm.imp.oozie


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config


// TODO: Add Oozie Validator
// TODO: Add validation of E1003:E1003: Invalid coordinator application attributes, Coordinator Start Time must be earlier than End Time.

object Linker {

	import io.github.datamoth.dm.api

	private val L = LoggerFactory.getLogger(classOf[Linker])


	def link(project: api.oozie.Project, ec: EntityCollector): api.oozie.Project = {
		val coordinators = project.coordinators.map(resolve(_, project, ec)).toList
		project.withCoordinators(coordinators)
	}

	private def resolve(c: api.oozie.Coordinator, p: api.oozie.Project, ec: EntityCollector): api.oozie.Coordinator = {
		resolveWorkflowFiles(resolveWorkflowVariables(
			resolveWorkflow(
				resolveVariables(
					resolveEvents(
						resolveIncludes(c, p, ec)
		, p, ec), p, ec), p, ec), p, ec), p, ec)
	}

	private def resolveWorkflowVariables(c: api.oozie.Coordinator, p: api.oozie.Project, ec: EntityCollector): api.oozie.Coordinator = {
		val vset = c.variables.map(_.name).toSet
		c.workflow.foreach { w =>
			w.variables.filter(v => !vset.contains(v.name)).foreach{ v =>
				ec.collectError(api.oozie.LinkError(
					location = Some(v.location)
					, message = s"Can't resolve variable: ${v.name} from coordinator ${c.name}"
				))
			}
		}
		c
	}

	private def resolveWorkflow(c: api.oozie.Coordinator, p: api.oozie.Project, ec: EntityCollector): api.oozie.Coordinator = {
		val candidates = p.workflows.flatMap{ w =>
			List((lcs(c.wpath.file, w.location.file), w)
				, (lcs(w.location.file, c.wpath.file), w))
		}
		if (candidates.isEmpty) {
			ec.collectError(api.oozie.LinkError(
				location = Some(c.location)
				, message = s"Can't resolve workflow: ${c.wpath.file}"
			))
			return c
		} else {
			val elected = candidates.maxBy(_._1.length)
			if (elected._2.location.file.endsWith(elected._1)) {
				return c.withWorkflow(elected._2)
			} else {
				ec.collectError(api.oozie.LinkError(
					location = Some(c.location)
					, message = s"Can't resolve workflow: ${c.wpath.file}"
				))
				return c
			}
		}
	}

	private def resolveVariables(c: api.oozie.Coordinator, p: api.oozie.Project, ec: EntityCollector): api.oozie.Coordinator = {
		val dati = ".*\\$\\{coord:dataIn\\('([a-zA-Z-_]+)'\\)".r
		val dato = ".*\\$\\{coord:dataOut\\('([a-zA-Z-_]+)'\\)".r
		val imap = c.inputs.map(_.name).toSet
		val omap = c.outputs.map(_.name).toSet
		val imiss = c.variables.flatMap{ v => dati.findFirstMatchIn(v.value).map(m => (v, m.group(1))) }
		val omiss = c.variables.flatMap{ v => dato.findFirstMatchIn(v.value).map(m => (v, m.group(1))) }
		imiss.filter(p => !imap.contains(p._2)).foreach{ p =>
			ec.collectError(api.oozie.LinkError(
				location = Some(p._1.location)
				, message = s"<Data-in> event not found: ${p._2}"
			))
		}
		omiss.filter(p => !omap.contains(p._2)).foreach{ p =>
			ec.collectError(api.oozie.LinkError(
				location = Some(p._1.location)
				, message = s"<Data-out> event not found: ${p._2}"
			))
		}
		c
	}

	private def resolveEvents(c: api.oozie.Coordinator, p: api.oozie.Project, ec: EntityCollector): api.oozie.Coordinator = {
		val dmap = c.includes
			.flatMap(_.databundle)
			.flatMap(i => (i.datasets ++ c.datasets).map(d => (d.name, d)) )
			.toMap
		val inputs = c.inputs.map(ev => ev.withDataset(dmap.get(ev.datasetName)))
		val outputs = c.outputs.map(ev => ev.withDataset(dmap.get(ev.datasetName)))
		inputs.filter(_.dataset.isEmpty).foreach(ev =>
			ec.collectError(api.oozie.LinkError(
				location = Some(ev.location)
				, message = s"Dataset not found neither in includes nor in datasets: ${ev.datasetName}"
			))
		)
		outputs.filter(_.dataset.isEmpty).foreach(ev =>
			ec.collectError(api.oozie.LinkError(
				location = Some(ev.location)
				, message = s"Dataset not found neither in includes nor in datasets: ${ev.datasetName}"
			))
		)
		c
			.withInputs(inputs)
			.withOutputs(outputs)
	}

	private def resolveIncludes(c: api.oozie.Coordinator, p: api.oozie.Project, ec: EntityCollector): api.oozie.Coordinator = {
		val includes = c.includes.map{ include =>
			val candidates = p.databundles.flatMap{ bundle =>
				List(
					(lcs(include.location.file, bundle.location.file), include, bundle)
					, (lcs(bundle.location.file, include.location.file), include, bundle)
				)
			}
			if (candidates.isEmpty) {
				ec.collectError(api.oozie.LinkError(
					location = Some(c.location)
					, message = s"Can't resolve include path: ${include.location.file}"
				))
				include
			} else {
				val elected = candidates.maxBy(_._1.length)
				if (elected._2.location.file.endsWith(elected._1)
						&& elected._3.location.file.endsWith(elected._1)) {
					elected._2.withDatabundle(elected._3)
				} else {
					ec.collectError(api.oozie.LinkError(
						location = Some(c.location)
						, message = s"Can't resolve include path: ${include.location.file}"
					))
					elected._2
				}
			}
		}
		c.withIncludes(includes)
	}

	private def resolveWorkflowFiles(c: api.oozie.Coordinator, p: api.oozie.Project, ec: EntityCollector): api.oozie.Coordinator = {
		val workflow = c.workflow.map{ w =>
			val files = w.files.map{ f =>
				val path = ec.bestPath(f.file)
				if (path == "") {
					ec.collectWarning(api.oozie.LinkError(
						location = Some(f)
						, message = s"Can't resolve file reference: ${f.file}"
					))
					f
				} else {
					f.copy(file = path)
				}
			}
			w.copy(files = files)
		}
		c.copy(workflow = workflow)
	}


	private def lcs(a: String, b: String) : String = {
		// Longest common substring
		def loop(m: Map[(Int, Int), Int], bestIndices: List[Int], i: Int, j: Int) : String = {
			if (i > a.length) {
				b.substring(bestIndices(1) - m((bestIndices(0),bestIndices(1))), bestIndices(1))
			} else if (i == 0 || j == 0) {
				loop(m + ((i,j) -> 0), bestIndices, if(j == b.length) i + 1 else i, if(j == b.length) 0 else j + 1)
			} else if (a(i-1) == b(j-1) && math.max(m((bestIndices(0),bestIndices(1))), m((i-1,j-1)) + 1) == (m((i-1,j-1)) + 1)) {
				loop(
					m + ((i,j) -> (m((i-1,j-1)) + 1)),
					List(i, j),
					if(j == b.length) i + 1 else i,
					if(j == b.length) 0 else j + 1
				)
			} else {
				loop(m + ((i,j) -> 0), bestIndices, if(j == b.length) i + 1 else i, if(j == b.length) 0 else j + 1)
			}
		}
		loop(Map[(Int, Int), Int](), List(0, 0), 0, 0)
	}

}

class Linker {
}
