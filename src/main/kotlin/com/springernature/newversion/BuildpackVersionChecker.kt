package com.springernature.newversion

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File


sealed class ChecksResult {
    abstract fun exitStatus(): Int
    abstract val detectedWorkflowUpdates: List<BuildpackUpdate>
}

data class SuccessfulChecks(
    val updates: List<BuildpackUpdate>,
    override val detectedWorkflowUpdates: List<BuildpackUpdate> = emptyList()
) : ChecksResult() {
    override fun exitStatus() = 0
}

data class FailedChecks(
    val updates: List<BuildpackUpdate>,
    val errors: Map<BuildpackUpdate, Exception>,
    override val detectedWorkflowUpdates: List<BuildpackUpdate> = emptyList()
) : ChecksResult() {
    override fun exitStatus() = 1
}

class BuildpackVersionChecker(
    private val manifestPath: File,
    private val buildpackUpdateChecker: BuildpackUpdateChecker,
    private val publisher: Publisher,
    private val settings: Settings = Settings()
) {

    fun performChecks(): ChecksResult {
        LOG.info("Performing checks")
        val errors = LinkedHashMap<BuildpackUpdate, Exception>()

        val cfBuildpacks = ManifestParser.load(manifestPath)
            .flatMap { ManifestBuildpack.from(it) }
            .map { ManifestEntry(it.manifest, it.buildpack) }

        val halfpipeBuildpacks = HalfpipeManifestParser.load(manifestPath)
            .flatMap { PaketoManifestBuildpack.from(it) }
            .map { ManifestEntry(it.manifest, it.buildpack) }

        val githubActionsBuildpacks = GitHubActionsManifestParser.load(manifestPath)
            .flatMap { PaketoManifestBuildpack.from(it) }
            .map { ManifestEntry(it.manifest, it.buildpack) }
            .toList()

        val updateWorkflowFiles = settings.lookup(Setting.UPDATE_WORKFLOW_FILES).toBoolean()

        val toPublish = cfBuildpacks + halfpipeBuildpacks +
                if (updateWorkflowFiles) githubActionsBuildpacks else emptyList()

        val updates = toPublish
            .filter { it.buildpack.version != Unparseable }
            .groupBy { it.buildpack }
            .map { (buildpack, entries) ->
                BuildpackUpdate(
                    entries.map { it.manifest },
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

        val detectedWorkflowUpdates = if (!updateWorkflowFiles) {
            githubActionsBuildpacks
                .filter { it.buildpack.version != Unparseable }
                .groupBy { it.buildpack }
                .mapNotNull { (buildpack, entries) ->
                    try {
                        BuildpackUpdate(
                            entries.map { it.manifest },
                            buildpack,
                            buildpackUpdateChecker.findLatestVersion(buildpack)
                        ).takeIf { it.hasUpdate() }
                    } catch (e: Exception) {
                        LOG.warn("Failed to check version for workflow buildpack {}: {}", buildpack.name, e.message)
                        null
                    }
                }
        } else emptyList()

        LOG.info("Done")
        return if (errors.isNotEmpty())
            FailedChecks(updates, errors, detectedWorkflowUpdates)
        else
            SuccessfulChecks(updates, detectedWorkflowUpdates)
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(BuildpackVersionChecker::class.java)
    }

    private data class ManifestEntry(val manifest: File, val buildpack: VersionedBuildpack)

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

    private data class PaketoManifestBuildpack(val manifest: File, val buildpack: VersionedBuildpack) {

        companion object {
            private val LOG: Logger = LoggerFactory.getLogger(PaketoManifestBuildpack::class.java)

            fun from(result: PaketoManifestLoadResult): List<PaketoManifestBuildpack> = when (result) {
                is FailedPaketoManifest -> {
                    LOG.warn("Failed to parse Paketo manifest {}: {}", result.path, result.error.message, result.error)
                    emptyList()
                }
                is PaketoManifest -> result.buildpacks.map { PaketoManifestBuildpack(result.path, it) }
            }
        }

    }

}
