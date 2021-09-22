package com.springernature.newversion

import java.io.File

data class BuildpackUpdate(
    val manifests: List<File>,
    val currentBuildpack: VersionedBuildpack,
    val latestUpdate: BuildpackVersion,
) {
    fun hasUpdate() = when (currentBuildpack.version) {
        is SemanticVersion -> currentBuildpack.version.toSemVer() < latestUpdate.version.toSemVer()
        is Latest -> false
    }
}
