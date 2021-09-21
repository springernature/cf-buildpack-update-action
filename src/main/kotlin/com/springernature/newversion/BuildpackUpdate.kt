package com.springernature.newversion

import java.io.File

data class BuildpackUpdate(
    val manifest: File,
    val currentBuildpack: VersionedBuildpack,
    val latestUpdate: BuildpackVersion,
) {
    fun hasUpdate() = when (currentBuildpack.version) {
        is SemanticVersion -> currentBuildpack.version.toSemVer() < latestUpdate.version.toSemVer()
        is Latest -> false
    }

    companion object {
        fun create(manifest: ManifestLoadResult, buildpackUpdateChecker: BuildpackUpdateChecker) = when (manifest) {
            is FailedManifest -> {
                println(manifest)
                emptyList()
            }
            is Manifest -> manifest.applications.flatMap { app -> app.buildpacks() }.map {
                BuildpackUpdate(manifest.path, it, buildpackUpdateChecker.findLatestVersion(it))
            }
        }
    }
}
