package com.springernature.newversion

import java.io.File

interface Publisher {
    fun publish(update: BuildpackUpdate)
}

class GitHubPullRequestPublisher(private val shell: Shell, settings: Settings) : Publisher {

    private val gitEmail: String = settings.lookup(Setting.AUTHOR_EMAIL)
    private val gitName: String = settings.lookup(Setting.AUTHOR_NAME)

    override fun publish(update: BuildpackUpdate) {
        createPullRequest(update.branchname(), update.commitMessage(), update.prMessage()) { updateManifest(update) }
    }

    private fun BuildpackUpdate.branchname() = "update-${currentBuildpack.name.replace('/', '-')}"

    private fun BuildpackUpdate.commitMessage() =
        "update ${currentBuildpack.name} to $latestVersion"

    private fun BuildpackUpdate.prMessage() =
        """
        update ${currentBuildpack.name} to $latestVersion in $manifestPath
        
        update ${currentBuildpack.name} from ${currentBuildpack.version} to $latestVersion
    """.trimIndent()

    private fun updateManifest(update: BuildpackUpdate) {
        println("updateManifest")
        val manifestContent = File(update.manifestPath).readText(Charsets.UTF_8)
        val newManifest =
            manifestContent.replace(
                "${update.currentBuildpack.name}#v${update.currentBuildpack.version}",
                "${update.currentBuildpack.name}#v${update.latestVersion}"
            )
        File(update.manifestPath).writeText(newManifest, Charsets.UTF_8)
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
                println("Branch already exist; skipping")
                return
            }
            createAndCheckoutBranch(branchName)
            makeChanges()
            commitChanges(commitMessage, gitName, gitEmail)
            createPullRequest(branchName, prMessage)
        } finally {
            switchToBranch(baseBranchName)
        }
    }

    private fun getBaseBranch(): String {
        return shell.run {
            git().currentBranch()
        }
    }

    private fun gitInit() {
        shell.run {
            git().init()
        }
    }

    private fun branchExistsAlready(name: String) =
        try {
            switchToBranch(name)
            true
        } catch (e: Exception) {
            false
        }

    private fun createAndCheckoutBranch(name: String) {
        shell.run {
            println("creating branch $name")
            git().checkout(name)
        }
    }

    private fun switchToBranch(name: String) {
        if (name.isBlank()) {
            throw RuntimeException("Cannot switch to empty branch name")
        }
        shell.run {
            git().switch(name)
        }
    }

    private fun commitChanges(message: String, name: String, email: String) {
        println("commitChanges")
        shell.run {
            git().commit(message, name, email)
        }
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
                    "--message='$prMessage'",
                    "--base=$name",
                    "--labels=buildpack-update"
                )
            )
        }
    }

    private fun Script.git() = GitCommands(this)

    private class GitCommands(private val script: Script) {
        fun init(): String {
            script.command("git", listOf("remote", "prune", "origin"))
            return script.command("git", listOf("fetch", "--prune", "--prune-tags"))
        }

        fun currentBranch() = script.command("git", listOf("rev-parse", "--abbrev-ref", "HEAD"))

        fun switch(branchName: String) = script.command("git", listOf("switch", branchName))

        fun commit(message: String, name: String, email: String) = script.command(
            "git", listOf(
                "commit", "-a", "--quiet",
                "--message", message,
                "--author", "$name <$email>",
            )
        )

        fun checkout(branchName: String) = script.command("git", listOf("checkout", "-B", branchName, "--quiet"))
    }
}
