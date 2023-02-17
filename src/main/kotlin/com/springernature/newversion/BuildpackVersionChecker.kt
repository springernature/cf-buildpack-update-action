package com.springernature.newversion

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File


sealed class ChecksResult {
    abstract fun exitStatus(): Int
}

data class SuccessfulChecks(val updates: List<SuccessResult>, val skippedResult: List<SkippedResult>) : ChecksResult() {
    override fun exitStatus() = 0
}

data class FailedChecks(val updates: List<PublishResult>, val errors: Map<BuildpackUpdate, FailureResult>) :
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
        val errors = LinkedHashMap<BuildpackUpdate, FailureResult>()
        val skipped = LinkedHashMap<BuildpackUpdate, SkippedResult>()
        val successes = LinkedHashMap<BuildpackUpdate, SuccessResult>()
        val updates: List<PublishResult> = ManifestParser.load(manifestPath)
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
            .map { publisher.publish(it) }
            .onEach {
                when(it) {
                    is SuccessResult -> successes[it.update] = it
                    is FailureResult -> errors[it.update] = it
                    is SkippedResult -> skipped[it.update] = it
                }
            }
        LOG.info("Done")
        return if (errors.isNotEmpty())
            FailedChecks(updates, errors)
        else
            SuccessfulChecks(successes.values.toList(), skipped.values.toList())
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
