package com.springernature.newversion

import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        private val LOG: Logger = LoggerFactory.getLogger(BuildpackUpdate::class.java)

        fun create(manifest: ManifestLoadResult, buildpackUpdateChecker: BuildpackUpdateChecker) = when (manifest) {
            is FailedManifest -> {
                LOG.error("Failed to parse manifest {}", manifest)
                emptyList()
            }
            is Manifest -> manifest.applications.flatMap { app -> app.buildpacks() }.map {
                BuildpackUpdate(manifest.path, it, buildpackUpdateChecker.findLatestVersion(it))
            }
        }
    }
}
