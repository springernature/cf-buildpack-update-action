package com.springernature.newversion

import net.swiftzer.semver.SemVer

interface Publisher {
    fun publish(update: BuildpackUpdate)
}

class GitHubPullRequestPublisher(private val shell: Shell, settings: Settings) : Publisher {

    private val gitEmail: String = settings.lookup(Setting.AUTHOR_EMAIL)
    private val gitName: String = settings.lookup(Setting.AUTHOR_NAME)

    override fun publish(update: BuildpackUpdate) {
        createPullRequest(update) { updateManifest(update) }
    }

    private fun BuildpackUpdate.baseBranchName() =
        "buildpack-update/${currentBuildpack.name.replace('/', '-')}"

    private fun BuildpackUpdate.branchName() = "${baseBranchName()}-$latestVersion"

    private fun BuildpackUpdate.commitMessage() =
        "Update ${currentBuildpack.name} to $latestVersion"

    private fun BuildpackUpdate.prMessage() =
        """
        Update ${currentBuildpack.name} to $latestVersion in $manifest
        
        Update ${currentBuildpack.name} from ${currentBuildpack.version} to $latestVersion
    """.trimIndent()

    private fun updateManifest(update: BuildpackUpdate) {
        println("Updating manifest for ${update.currentBuildpack.name}#v${update.currentBuildpack.version} -> v${update.latestVersion}")
        val manifestContent = update.manifest.readText(Charsets.UTF_8)
        val newManifest =
            manifestContent.replace(
                "${update.currentBuildpack.name}#v${update.currentBuildpack.version}",
                "${update.currentBuildpack.name}#v${update.latestVersion}"
            )
        update.manifest.writeText(newManifest, Charsets.UTF_8)
    }

    private fun createPullRequest(
        update: BuildpackUpdate,
        makeChanges: () -> Unit
    ) {
        gitInit(gitName, gitEmail)

        val baseBranchName = getBaseBranch()
        val prBranchNames = openPullRequestBranchNames()

        try {
            if (pullRequestForBranchExists(update.branchName(), prBranchNames)) {
                println("Branch ${update.branchName()} already exists; skipping")
                return
            }
            createAndCheckoutBranch(update.branchName())
            makeChanges()
            commitChanges(update.commitMessage(), gitName, gitEmail)
            push(update.branchName())
            createPullRequest(update.prMessage())

            cleanUpOldPullRequests(update.baseBranchName(), update.latestVersion, prBranchNames)

        } finally {
            switchToBranch(baseBranchName)
        }
    }

    private fun getBaseBranch(): String = shell.run {
        git().currentBranch()
    }

    private fun gitInit(defaultCommitterName: String, defaultCommitterEmail: String) {
        shell.run {
            git().init()
            git().configUserName(defaultCommitterName)
            git().configUserEmail(defaultCommitterEmail)
        }
    }

    private fun pullRequestForBranchExists(branchName: String, prBranchNames: List<String>) = prBranchNames
        .contains(branchName)
        .also {
            if (it)
                println("Branch $branchName already exists")
            else
                println("No branch named $branchName exists")
        }

    private fun openPullRequestBranchNames() = shell
            .run {
                command("hub", listOf("pr", "list", "-s", "open", "-f", "'%H%n'"))
            }
            .split("\n")
            .map { it.trim() }

    private fun createAndCheckoutBranch(name: String) {
        shell.run {
            println("Creating branch $name")
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
        println("Committing changes as $name <$email>")
        shell.run {
            git().commit(message, name, email)
        }
    }

    private fun push(branchName: String) {
        shell.run {
            git().pushAndSetUpstream(branchName)
        }
    }

    private fun createPullRequest(prMessage: String) {
        println("Creating pull request")
        shell.run {
            command(
                "hub",
                listOf(
                    "pull-request",
                    "--push",
                    "--message='$prMessage'",
                    "--labels=buildpack-update"
                )
            )
        }
    }

    private fun cleanUpOldPullRequests(baseBranchName: String, currentVersion: SemanticVersion, prBranchNames: List<String>) {
        println("Cleaning up old PRs for $baseBranchName")
        prBranchNames
            .filter { it.startsWith(baseBranchName) }
            .filter { SemVer.parse(it.substringAfterLast("-")) < currentVersion.toSemVer() }
            .forEach {
                println("- Deleting old PR branch $it")
                shell.run {
                    git().deleteRemoteBranch(it)
                }
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
                "-m", message,
                "--author='$name <$email>'"
            )
        )

        fun checkout(branchName: String) = script.command("git", listOf("checkout", "-B", branchName, "--quiet"))

        fun deleteRemoteBranch(branchName: String) = script.command("git", listOf("push", "origin", ":$branchName"))

        fun pushAndSetUpstream(branchName: String) = script.command(
            "git", listOf("push", "--set-upstream", "origin", branchName)
        )

        fun configUserName(name: String) =script.command(
            "git", listOf("config", "user.name", name)
        )

        fun configUserEmail(email: String) =script.command(
            "git", listOf("config", "user.email", email)
        )
    }
}
