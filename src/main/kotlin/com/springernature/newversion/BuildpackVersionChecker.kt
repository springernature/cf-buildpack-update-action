package com.springernature.newversion

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File


sealed class ChecksResult {
    abstract fun exitStatus(): Int
}

data class SuccessfulChecks(val updates: List<BuildpackUpdate>) : ChecksResult() {
    override fun exitStatus() = 0
}

data class FailedChecks(val updates: List<BuildpackUpdate>, val errors: Map<BuildpackUpdate, Exception>) :
    ChecksResult() {
    override fun exitStatus() = 1
}

class BuildpackVersionChecker(
    private val manifestPath: File,
    private val buildpackUpdateChecker: BuildpackUpdateChecker,
    private val publisher: Publisher
) {

    fun performChecks(): ChecksResult {
        LOG.info("Performing checks")
        val errors = LinkedHashMap<BuildpackUpdate, Exception>()
        val updates = ManifestParser.load(manifestPath)
            .flatMap { ManifestBuildpack.from(it) }
            .filter { it.buildpack.version != Unparseable }
            .groupBy { it.buildpack }
            .map { (buildpack, manifestBuildpacks) ->
                BuildpackUpdate(
                    manifestBuildpacks.map { it.manifest },
                    buildpack,
                    buildpackUpdateChecker.findLatestVersion(buildpack)
                )
            }
            .filter(BuildpackUpdate::hasUpdate)
            .onEach {
                try {
                    publisher.publish(it)
                } catch (e: Exception) {
                    errors[it] = e
                }
            }
        LOG.info("Done")
        return if (errors.isNotEmpty())
            FailedChecks(updates, errors)
        else
            SuccessfulChecks(updates)
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
