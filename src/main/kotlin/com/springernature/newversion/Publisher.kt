package com.springernature.newversion

import net.swiftzer.semver.SemVer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

sealed class PublishResult
data class SuccessResult(val update: BuildpackUpdate) : PublishResult()
data class FailureResult(val update: BuildpackUpdate, val reason: String) : PublishResult()
data class SkippedResult(val update: BuildpackUpdate, val reason: String) : PublishResult()

interface Publisher {
    fun publish(update: BuildpackUpdate): PublishResult
}

class GitHubPullRequestPublisher(private val shell: Shell, settings: Settings) : Publisher {

    private val gitEmail: String = settings.lookup(Setting.AUTHOR_EMAIL)
    private val gitName: String = settings.lookup(Setting.AUTHOR_NAME)

    override fun publish(update: BuildpackUpdate): PublishResult =
        createPullRequest(update) { updateManifest(update) }

    private fun BuildpackUpdate.baseBranchName() =
        "buildpack-update/${currentBuildpack.name.replace('/', '-')}"

    private fun BuildpackUpdate.branchName() = "${baseBranchName()}-${currentBuildpack.version}-${latestUpdate.version}"

    private fun BuildpackUpdate.commitMessage() =
        "Update ${currentBuildpack.name} to ${latestUpdate.version}"

    private fun BuildpackUpdate.pullRequestMessage() =
        """
        Update ${currentBuildpack.name} to ${latestUpdate.version}
        
        Update ${currentBuildpack.name} from ${currentBuildpack.version} to ${latestUpdate.version}.
        
        * [Release Notes](https://github.com/${currentBuildpack.name}/releases/tag/${latestUpdate.tag.value})
        * [Diff](https://github.com/${currentBuildpack.name}/compare/${currentBuildpack.tag?.value}...${latestUpdate.tag.value})
    """.trimIndent()

    private fun updateManifest(update: BuildpackUpdate) {
        update.manifests.forEach { manifest ->
            LOG.info(
                "Updating manifest $manifest for {}#{} -> {}",
                update.currentBuildpack.name, update.currentBuildpack.tag?.value, update.latestUpdate.tag.value
            )
            val manifestContent = manifest.readText(Charsets.UTF_8)
            val newManifest =
                manifestContent.replace(
                    "${update.currentBuildpack.name}#${update.currentBuildpack.tag?.value}",
                    "${update.currentBuildpack.name}#${update.latestUpdate.tag.value}"
                ).replace(
                    "${update.currentBuildpack.name}.git#${update.currentBuildpack.tag?.value}",
                    "${update.currentBuildpack.name}.git#${update.latestUpdate.tag.value}"
                )
            manifest.writeText(newManifest, Charsets.UTF_8)
        }
    }

    private fun createPullRequest(
        update: BuildpackUpdate,
        makeChanges: () -> Unit
    ): PublishResult {
        gitInit(gitName, gitEmail)

        val baseBranchName = getBaseBranch()
        val prBranchNames = remoteBranchNames()

        try {
            if (pullRequestForBranchExists(update.branchName(), prBranchNames)) {
                LOG.info("The PR branch {} already exists; skipping", update.branchName())
                return SkippedResult(update, "The PR branch ${update.branchName()} already exists; skipping")
            }
            createAndCheckoutBranch(update.branchName())
            makeChanges()
            commitChanges(update.commitMessage(), gitName, gitEmail)
            push(update.branchName())
            createPullRequest(update.pullRequestMessage(), baseBranchName)

            cleanUpOldPullRequests(update.baseBranchName(), update.latestUpdate.version, prBranchNames)
        } catch (e: Exception) {
            return SuccessResult(update)
        } finally {
            switchToBranch(baseBranchName)
        }

        return SuccessResult(update)
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

    private fun remoteBranchNames() = shell
        .run {
            command("git", listOf("branch", "-r"))
        }
        .split("\n")
        .map { it.trim() }
        .map { it.replaceFirst(Regex("^origin/"), "") }
        .map { it.split(' ').first().trim() }

    private fun createAndCheckoutBranch(name: String) {
        shell.run {
            LOG.info("Creating branch {}", name)
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
        LOG.info("Committing changes as '{} <{}>'", name, email)
        shell.run {
            git().commit(message, name, email)
        }
    }

    private fun push(branchName: String) {
        shell.run {
            git().pushAndSetUpstream(branchName)
        }
    }

    private fun createPullRequest(
        prMessage: String,
        baseBranchName: String
    ) {
        LOG.info("Creating pull request from base {}", baseBranchName)
        shell.run {
            command(
                "hub",
                listOf(
                    "pull-request",
                    "--push",
                    "--message", prMessage,
                    "--base", baseBranchName,
                    "--labels", "buildpack-update"
                )
            )
        }
    }

    private fun cleanUpOldPullRequests(
        baseBranchName: String,
        currentVersion: SemanticVersion,
        prBranchNames: List<String>
    ) {
        LOG.info("Cleaning up old PRs for {}", baseBranchName)
        prBranchNames
            .filter { it.startsWith(baseBranchName) }
            .filter { SemVer.parse(it.substringAfterLast("-")) < currentVersion.toSemVer() }
            .forEach {
                LOG.info("Deleting old PR branch {}", it)
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
                "--author", "$name <$email>"
            )
        )

        fun checkout(branchName: String) = script.command("git", listOf("checkout", "-B", branchName, "--quiet"))

        fun deleteRemoteBranch(branchName: String) = script.command("git", listOf("push", "origin", ":$branchName"))

        fun pushAndSetUpstream(branchName: String) = script.command(
            "git", listOf("push", "--set-upstream", "origin", branchName)
        )

        fun configUserName(name: String) = script.command(
            "git", listOf("config", "user.name", name)
        )

        fun configUserEmail(email: String) = script.command(
            "git", listOf("config", "user.email", email)
        )
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GitHubPullRequestPublisher::class.java)
    }
}
