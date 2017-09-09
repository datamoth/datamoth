package io.github.datamoth.dm.imp

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.nio.charset.StandardCharsets
import org.apache.commons.io.FileUtils

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.eclipse.jgit.api.ListBranchCommand.ListMode
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import org.eclipse.jgit.api.errors.CheckoutConflictException
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.transport.RefSpec


object Repository {

	case class Config(
		local: File
		, origin: String
	)

	def create(local: File, origin: String): Repository = {
		new Repository(Config(
			local = local
			, origin = origin
		))
	}


	def init(namespace: String, project: String, workDir: String, origin: File): Unit = {
		import java.nio.file.Paths
		import java.io.FileWriter
		import java.io.BufferedWriter
		import org.apache.commons.io.IOUtils
		AsResource(Git.init().setDirectory(origin).setBare(true).call())
		val updateHookRaw = IOUtils.toString(getClass.getClassLoader.getResourceAsStream("githooks/update"), StandardCharsets.UTF_8)
		val postReceiveHookRaw = IOUtils.toString(getClass.getClassLoader.getResourceAsStream("githooks/post-receive"), StandardCharsets.UTF_8)
		val updateHook = updateHookRaw
							.replaceAll("\\{\\{NAMESPACE\\}\\}", namespace)
							.replaceAll("\\{\\{PROJECT\\}\\}", project)
		val postReceiveHook = postReceiveHookRaw
								.replaceAll("\\{\\{NAMESPACE\\}\\}", namespace)
								.replaceAll("\\{\\{PROJECT\\}\\}", project)
								.replaceAll("\\{\\{WORKDIR\\}\\}", workDir)
		val updateF = Paths.get(origin.toString, "hooks", "update").toFile
		val postReceiveF = Paths.get(origin.toString, "hooks", "post-receive").toFile
		updateF.createNewFile()
		postReceiveF.createNewFile()
		updateF.setExecutable(true)
		postReceiveF.setExecutable(true)
		AsResource(new BufferedWriter(new FileWriter(updateF))).foreach{ f =>
			f.write(updateHook)
		}
		AsResource(new BufferedWriter(new FileWriter(postReceiveF))).foreach{ f =>
			f.write(postReceiveHook)
		}
	}

}

class Repository(cfg: Repository.Config) {

	import io.github.datamoth.dm.api
	import collection.JavaConverters._

	// TODO: Carefully rework decoding byte arrays to strings

	private val L = LoggerFactory.getLogger(classOf[Repository])

	def sync(): Unit = {
		val local = cfg.local.getParentFile
		L.debug("Sync: {}", local)
		def clone() {
			if (!cfg.local.exists()) {
				L.debug("{} doesn't exist, create", local)
				local.mkdirs()
				try {
					L.debug("Clone: {}", cfg.origin)
					Git.cloneRepository()
						.setURI(cfg.origin)
						.setDirectory(local)
						.call()
				} catch {
					case e: Exception => {
						L.debug("Exception during clone: {}", e.getMessage)
						throw e
					}
				}
			}
		}
		clone()
		for (git <- AsResource(Git.open(cfg.local))) {
			try {
				L.debug("Repo exists try to reset --hard")
				git.reset().setMode(ResetType.HARD).call()
				val specs = new java.util.ArrayList[RefSpec]()
				specs.add(new RefSpec("+refs/heads/*:refs/remotes/origin/*"));
				specs.add(new RefSpec("+refs/tags/*:refs/tags/*"));
				specs.add(new RefSpec("+refs/notes/*:refs/notes/*"));
				L.debug("Reset ok, try to fetch")
				git.fetch.setRefSpecs(specs).call()
				L.debug("Fetch ok")
			} catch {
				// Maybe our local repo corrupted. Try to clone and resync
				case e: Exception => {
					L.debug("Exception during reset and pull:", e)
					L.debug("Delete: {}", local)
					FileUtils.deleteQuietly(local)
					clone()
					git.pull.call()
				}
			}
		}
	}

	def branches(trySync: Boolean = true): List[api.git.Branch] = {
		L.debug("Get branches list in {}", cfg.local)
		try {
			AsResource(Git.open(cfg.local)).map { git =>
				git.branchList
					.setListMode(ListMode.ALL)
					.call
					.asScala
					.toList
					.map{ b => api.git.Branch(b.getName) }
					.filter{ b => b.name.startsWith("refs/remotes") }
			}
		} catch {
			case e: Exception =>
				L.debug("Exception during branch listing:", e)
				throw e
		}
	}

	def commitLog(ref: String, depth: Int): Seq[api.git.Commit] = {
		import java.time.LocalDateTime
		import java.time.ZoneId
		import java.time.Instant
		AsResource(Git.open(cfg.local)).map { git =>
			val objectId = git.getRepository.resolve(ref)
			if (objectId != null) {
				git.log.add(objectId).call.asScala.take(depth).map { c =>
					val authorDT = c.getAuthorIdent.getWhen.toInstant.atZone(ZoneId.systemDefault()).toLocalDateTime.toString
					val commitDT = c.getAuthorIdent.getWhen.toInstant.atZone(ZoneId.systemDefault()).toLocalDateTime.toString
					api.git.Commit(
						id = c.getId.toString
						, author = c.getAuthorIdent.getName
						, authorEmail = c.getAuthorIdent.getEmailAddress
						, committer = c.getCommitterIdent.getName
						, committerEmail = c.getCommitterIdent.getEmailAddress
						, msg = c.getFullMessage
						, authorDateTime = authorDT
						, commitDateTime = commitDT
					)
				}.toSeq
			} else {
				Seq()
			}
		}
	}

	def commitIdFor(refName: String): String = {
		AsResource(Git.open(cfg.local)).map { git =>
			val ref = git.getRepository.findRef(refName)
			if (ref == null) {
				val msg = s"Ref not found: ${refName}"
				L.error(msg)
				throw new IllegalStateException(msg)
			} else {
				ref.getObjectId.getName
			}
		}
	}

	def checkoutAndTrack(ref: String): Unit = {
		val localBranchName = ref.split('/').last
		AsResource(Git.open(cfg.local)).map { git =>
			try {
				// We need this in case when we need to remove the localBranch
				// see handling exceptions below: since we can't remove it
				// when standing on it
				git.checkout().setName("refs/remotes/origin/master").call()
			} catch {
				case e: Exception =>
					L.debug("Exception during checkout:", e)
			}
			L.debug("Checkout and track: {} -> {}", ref:Any, localBranchName)
			git.clean().setForce(true).call()
			val cmd = git.checkout()
				.setCreateBranch(true)
				.setName(localBranchName)
				.setStartPoint(ref)
				.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
			try {
				cmd.call()
			} catch {
				case e @ (_: RefAlreadyExistsException | _: CheckoutConflictException) => {
					L.debug("Error while checkout {}:{}", localBranchName:Any, e.getMessage)
					L.debug("Delete branch: {}", localBranchName)
					L.debug("Try to reset --hard")
					git.reset().setMode(ResetType.HARD).call()
					git.branchDelete().setBranchNames(localBranchName).setForce(true).call()
					L.debug("Checkout and track: {} -> {}", ref:Any, localBranchName)
					cmd.call()
				}
			}
		}
	}

	def commitTagAndPush(tag: String, msg: String): Unit = {
		AsResource(Git.open(cfg.local)).map { git =>
			L.debug("Commit state changes")
			git.commit()
				.setAll(true)
				.setAuthor("datamot", "datamot@datamot.home")
				.setCommitter("datamot", "datamot@datatmot.github.io")
				.setMessage(msg)
				.call()
			git.push()
				.setPushAll()
				.call()
		}
	}

	def readFile(ref: String, path: String): String = {
		L.debug("Try to read file from ref: {}:{}", ref:Any, path)
		AsResource(Git.open(cfg.local)).map{ git =>
			val repository = git.getRepository
			val commitId = repository.resolve(ref)
			if (commitId == null) {
				val msg = s"Ref not found: ${ref}"
				L.error(msg)
				throw new IllegalStateException(msg)
			}
			L.debug("Ref resolved to id: {}:{}", ref:Any, commitId.toString)
			AsResource(new RevWalk(repository)).map{ revWalk =>
				val commit = revWalk.parseCommit(commitId)
				val tree = commit.getTree
				AsResource(new TreeWalk(repository)).map{ treeWalk =>
					treeWalk.addTree(tree)
					treeWalk.setRecursive(true)
					treeWalk.setFilter(PathFilter.create(path))
					if (!treeWalk.next()) {
						val msg = s"File not found: ${ref}:${path}"
						L.error(msg)
						throw new IllegalStateException(msg)
					}
					val id = treeWalk.getObjectId(0)
					val loader = repository.open(id)
					new String(loader.getBytes, StandardCharsets.UTF_8)
				}
			}
		}
	}

}
