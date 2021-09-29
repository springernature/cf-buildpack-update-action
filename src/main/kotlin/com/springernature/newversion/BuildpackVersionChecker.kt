package com.springernature.newversion

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class BuildpackVersionChecker(
    private val manifestPath: File,
    private val buildpackUpdateChecker: BuildpackUpdateChecker,
    private val publisher: Publisher
) {

    fun performChecks() {
        ManifestParser.load(manifestPath)
            .flatMap { ManifestBuildpack.from(it) }
            .filter { it.buildpack.version != Latest }
            .groupBy { it.buildpack }
            .map { (buildpack, manifestBuildpacks) ->
                BuildpackUpdate(manifestBuildpacks.map { it.manifest }, buildpack, buildpackUpdateChecker.findLatestVersion(buildpack))
            }
            .filter(BuildpackUpdate::hasUpdate)
            .forEach {
                try {
                    publisher.publish(it)
                } catch (e: Exception) {
                    LOG.error("Publish of update failed: {}", it, e)
                }
            }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(BuildpackVersionChecker::class.java)
    }

    private data class ManifestBuildpack(val manifest: File, val buildpack: VersionedBuildpack) {

        companion object {
            private val LOG: Logger = LoggerFactory.getLogger(BuildpackUpdate::class.java)

            fun from(manifest: ManifestLoadResult) = when (manifest) {
                is FailedManifest -> {
                    LOG.error("Failed to parse manifest {}", manifest)
                    emptyList()
                }
                is Manifest -> manifest.applications.flatMap { app -> app.buildpacks() }.map {
                    ManifestBuildpack(manifest.path, it)
                }
            }
        }

    }

}
