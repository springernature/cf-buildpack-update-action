package com.springernature.newversion

import com.lordcodes.turtle.GitCommands
import java.io.File

interface Publisher {
    fun publish(update: BuildpackUpdate)
}

class GitHubPullRequestPublisher(private val shell: Shell, settings: Settings) : Publisher {

    private val gitEmail: String = settings.lookup(Setting.AUTHOR_EMAIL)
    private val gitName: String = settings.lookup(Setting.AUTHOR_NAME)

    override fun publish(update: BuildpackUpdate) {
        createPullRequest(update.branchname(), update.commitMessage(), update.prMessage(), updateManifest(update))
    }

    private fun BuildpackUpdate.branchname() = "update-${currentBuildpack.name.replace('/', '-')}"

    private fun BuildpackUpdate.commitMessage() =
        "update ${currentBuildpack.name} to $latestVersion"

    private fun BuildpackUpdate.prMessage() =
        """
        update ${currentBuildpack.name} to $latestVersion in $manifestPath
        
        update ${currentBuildpack.name} from ${currentBuildpack.version} to $latestVersion
    """.trimIndent()

    private fun updateManifest(update: BuildpackUpdate): () -> Unit {
        println("updateManifest")
        update.apply {
            return {
                val manifestContent = File(manifestPath).readText(Charsets.UTF_8)
                val newManifest =
                    manifestContent.replace(
                        "${currentBuildpack.name}#v${currentBuildpack.version}",
                        "${currentBuildpack.name}#v${latestVersion}"
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
        return shell.run {
            git.currentBranch()
        }
    }

    private fun gitInit() {
        shell.run {
            command("git", listOf("remote", "prune", "origin"))
            command("git", listOf("fetch", "--prune", "--prune-tags"))
        }
    }

    private fun branchExistsAlready(name: String) =
        try {
            shell.run {
                git.gitCommand(listOf("switch", name))
            }
            true
        } catch (e: Exception) {
            false
        }

    private fun createBranchIfMissing(name: String) {
        shell.run {
            println("creating branch $name")
            git.checkout(name, true)
        }
    }

    private fun switchToBranch(name: String) {
        shell.run {
            git.gitCommand(listOf("switch", name))
        }
    }

    private fun commitChanges(message: String, name: String, email: String) {
        println("commitChanges")
        shell.run {
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
        shell.run {
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
