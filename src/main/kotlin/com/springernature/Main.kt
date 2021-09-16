package com.springernature

import com.lordcodes.turtle.GitCommands
import com.lordcodes.turtle.ShellRunException
import com.lordcodes.turtle.shellRun
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

val gitHubApi: String = System.getenv("GITHUB_API_URL") ?: "https://api.github.com"
val gitEmail: String = System.getenv("AUTHOR_EMAIL") ?: "do_not_reply@springernature.com"
val gitName: String = System.getenv("AUTHOR_NAME") ?: "buildpack update action"

val baseBranchName = getBaseBranch()

fun main() {

    gitInit()

    println("manifest file candidates found:")

    // using extension function walk
    File(".")
        .walk()
        .filterNot { it.path.contains("\\.git/") }
        .filter { it.name.contains("manifest") }
        .map(::toManifestCandidate)
        .filter(ManifestCandidate::isEligible)
        .flatMap(::toBuildpackCandidates)
        .filter(BuildpackCandidate::isEligible)
        //.apply { this.forEach { println("${it.manifestCandidate.file}: ${it.manifestLine.trim()}") } }
        .map(::toUpdateCandidate)
        // .apply { this.forEach { println("${it.buildpackCandidate.manifestCandidate.file}: ${it.buildpackCandidate.manifestLine.trim()} ${it.buildpackCandidate.currentVersion} ${it.latestVersion}") } }
        .filter(UpdateCandidate::isEligible)
        .apply { this.forEach { println("${it.buildpackCandidate.manifestCandidate.file}: ${it.buildpackCandidate.manifestLine.trim()}") } }
        .map(::toUpdateBranch)
        .filter(UpdateBranch::isEligible)
        .forEach {
            it.update()
        }
}

data class UpdateBranch(val updateCandidate: UpdateCandidate, val updateBranchName: String) {

    fun isEligible(): Boolean {
        println("update branch: $updateBranchName")
        return try {
            switchToExistingBranch(updateBranchName)
            println("branch already exists: $updateBranchName")
            false
        } catch (e: ShellRunException) {
            println("branch does not exist: $updateBranchName")
            true
        } finally {
            switchToExistingBranch(baseBranchName)
        }
    }

    private fun switchToExistingBranch(targetBranchName: String) {
        shellRun {
            git.gitCommand(listOf("switch", targetBranchName))
        }
    }

    private fun createUpdateBranch() {
        println("current branch: $baseBranchName")
        shellRun {
            println("creating branch $updateBranchName")
            git.checkout(updateBranchName, true)
        }
    }

    private fun updateManifest() {
        println("updateManifest")
        val buildpackCandidate = updateCandidate.buildpackCandidate
        val newManifest =
            buildpackCandidate.manifestCandidate.manifest.replace(
                "${buildpackCandidate.buildpackName}#v${buildpackCandidate.currentVersion}",
                "${buildpackCandidate.buildpackName}#v${updateCandidate.latestVersion}"
            )
        buildpackCandidate.manifestCandidate.file.writeText(newManifest, Charsets.UTF_8)
    }

    private fun createPullRequest() {
        println("createPullRequest")
        // https://hub.github.com/hub-pull-request.1.html
        shellRun {
            command(
                "hub",
                listOf(
                    "pull-request",
                    "--push",
                    "--message=Update ${updateCandidate.buildpackCandidate.buildpackName} to ${updateCandidate.latestVersion}",
                    "--base=$baseBranchName",
                    "--labels=buildpack-update"
                )
            )
        }
    }

    fun update() {
        try {
            createUpdateBranch()
            updateManifest()
            commitChanges()
            createPullRequest()
        } finally {
            switchToExistingBranch(baseBranchName)
        }
    }

    private fun commitChanges() {
        println("commitChanges")
        shellRun {
            with(updateCandidate) {
                git.commit(
                    "update ${buildpackCandidate.buildpackName} from version ${buildpackCandidate.currentVersion} to $latestVersion",
                    gitName, gitEmail
                )
            }
        }
    }

}

fun GitCommands.commit(message: String, name: String, email: String): String {
    return gitCommand(
        listOf(
            "commit",
            "--message", message,
            "--author", "$name <$email>",
        )
    )
}

fun toUpdateBranch(updateCandidate: UpdateCandidate): UpdateBranch {
    return UpdateBranch(updateCandidate, "update-${updateCandidate.buildpackCandidate.buildpackName.replace('/', '-')}")
}

data class UpdateCandidate(val buildpackCandidate: BuildpackCandidate, val latestVersion: String) {
    fun isEligible() = buildpackCandidate.currentVersion < latestVersion
}

fun toUpdateCandidate(buildpackCandidate: BuildpackCandidate): UpdateCandidate {
    val client = HttpClient.newBuilder().build()
    val url = "$gitHubApi/repos/${buildpackCandidate.buildpackName}/releases/latest"
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url)).build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    val message = response.body()
    // "tag_name":"v1.5.24"
    """"tag_name":"v([^"]*)"""".toRegex().find(message)?.let {
        val latestVersion = it.groups[1]!!.value
        return UpdateCandidate(buildpackCandidate, latestVersion)
    }
    return UpdateCandidate(buildpackCandidate, buildpackCandidate.currentVersion)
}

data class ManifestCandidate(val file: File, val manifest: String) {
    fun isEligible() = manifest.contains("buildpacks:")
            && manifest.contains("-buildpack#")
}

fun toManifestCandidate(m: File): ManifestCandidate {
    return ManifestCandidate(m, m.readText(Charsets.UTF_8))
}

data class BuildpackCandidate(val manifestCandidate: ManifestCandidate, val manifestLine: String) {

    fun isEligible() = ".*github.com/.*#v.*".toRegex().matches(manifestLine)

    val buildpackName: String by lazy {
        ".*github.com/(.*)#v.*".toRegex().find(manifestLine)!!.groups[1]!!.value
    }

    val currentVersion: String by lazy {
        ".*github.com/.*#v(.*)".toRegex().find(manifestLine)!!.groups[1]!!.value
    }

}

fun toBuildpackCandidates(manifestCandidate: ManifestCandidate): Collection<BuildpackCandidate> {
    return manifestCandidate.manifest.lines().filter { it.contains("-buildpack#") }.toSet()
        .map { BuildpackCandidate(manifestCandidate, it) }
}

fun getBaseBranch(): String {
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
