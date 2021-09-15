package com.springernature.newversion

import java.io.File

fun main() {

    loadManifests(".")
        .flatMap(::toBuildpackUpdate)
        .filter(BuildpackUpdate::hasUpdate)
        .forEach { update ->
            createPullRequest(update.branchname(), update.commitMessage(), update.prMessage(), update::updateManifest)
        }

}

private fun BuildpackUpdate.hasUpdate() = when (currentBuildpack.version) {
    is SemanticVersion -> when (latestBuildpack.version) {
        is SemanticVersion -> currentBuildpack.version.toSemVer() < latestBuildpack.version.toSemVer()
        else -> false
    }
    is Latest -> false
}

private fun BuildpackUpdate.branchname() = "update-${currentBuildpack.name.replace('/', '-')}"

private fun BuildpackUpdate.commitMessage() =
    "update ${currentBuildpack.name} to ${latestBuildpack.version}"

private fun BuildpackUpdate.prMessage() =
    """
        update ${currentBuildpack.name} to ${latestBuildpack.version} in $manifestPath
        
        update ${currentBuildpack.name} from ${currentBuildpack.version} to ${latestBuildpack.version}
    """.trimIndent()

private fun BuildpackUpdate.updateManifest() {
    println("updateManifest")
    val manifestContent = File(manifestPath).readText(Charsets.UTF_8)
    val newManifest =
        manifestContent.replace(
            "${currentBuildpack.name}#v${currentBuildpack.name}",
            "${currentBuildpack.name}#v${latestBuildpack.name}"
        )
    File(manifestPath).writeText(newManifest, Charsets.UTF_8)
}
