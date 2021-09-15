package com.springernature.newversion

import com.lordcodes.turtle.GitCommands
import com.lordcodes.turtle.shellRun
import java.io.File

class GitHubPullRequestPublisher(settings: Settings) : Publisher {

    private val gitEmail: String = settings.lookup(Setting.AUTHOR_EMAIL)
    private val gitName: String = settings.lookup(Setting.AUTHOR_NAME)

    override fun publish(update: BuildpackUpdate) {
        createPullRequest(update.branchname(), update.commitMessage(), update.prMessage(), updateManifest(update))
    }

    private fun BuildpackUpdate.branchname() = "update-${currentBuildpack.name.replace('/', '-')}"

    private fun BuildpackUpdate.commitMessage() =
        "update ${currentBuildpack.name} to ${latestBuildpack.version}"

    private fun BuildpackUpdate.prMessage() =
        """
        update ${currentBuildpack.name} to ${latestBuildpack.version} in $manifestPath
        
        update ${currentBuildpack.name} from ${currentBuildpack.version} to ${latestBuildpack.version}
    """.trimIndent()

    private fun updateManifest(update: BuildpackUpdate): () -> Unit {
        println("updateManifest")
        update.apply {
            return {
                val manifestContent = File(manifestPath).readText(Charsets.UTF_8)
                val newManifest =
                    manifestContent.replace(
                        "${currentBuildpack.name}#v${currentBuildpack.name}",
                        "${currentBuildpack.name}#v${latestBuildpack.name}"
                    )
                File(manifestPath).writeText(newManifest, Charsets.UTF_8)
            }
        }
    }

    private fun createPullRequest(
        branchName: String,
        commitMessage: String,
        prMessage: String,
        makeChanges: () -> Unit
    ) {
        val baseBranchName = getBaseBranch()
        gitInit()
        try {
            if (branchExistsAlready(branchName)) {
                return
            }
            createBranchIfMissing(branchName)
            switchToBranch(branchName)
            makeChanges()
            commitChanges(commitMessage, gitName, gitEmail)
            createPullRequest(branchName, prMessage)
        } finally {
            switchToBranch(baseBranchName)
        }
    }

    private fun getBaseBranch(): String {
        return shellRun {
            git.currentBranch()
        }
    }

    private fun gitInit() {
        shellRun {
            command("git", listOf("remote", "prune", "origin"))
            command("git", listOf("fetch", "--prune", "--prune-tags"))
        }
    }

    private fun branchExistsAlready(name: String) =
        try {
            shellRun {
                git.gitCommand(listOf("switch", name))
            }
            true
        } catch (e: Exception) {
            false
        }

    private fun createBranchIfMissing(name: String) {
        shellRun {
            println("creating branch $name")
            git.checkout(name, true)
        }
    }

    private fun switchToBranch(name: String) {
        shellRun {
            git.gitCommand(listOf("switch", name))
        }
    }

    private fun commitChanges(message: String, name: String, email: String) {
        println("commitChanges")
        shellRun {
            git.commit(message, name, email)
        }
    }

    private fun GitCommands.commit(message: String, name: String, email: String): String {
        return gitCommand(
            listOf(
                "commit",
                "--message", message,
                "--author", "$name <$email>",
            )
        )
    }

    private fun createPullRequest(name: String, prMessage: String) {
        println("createPullRequest")
        // https://hub.github.com/hub-pull-request.1.html
        shellRun {
            command(
                "hub",
                listOf(
                    "pull-request",
                    "--push",
                    "--message=$prMessage",
                    "--base=$name",
                    "--labels=buildpack-update"
                )
            )
        }
    }
}
