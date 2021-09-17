package com.springernature.newversion

data class BuildpackUpdate(
    val manifestPath: String,
    val currentBuildpack: VersionedBuildpack,
    val latestVersion: SemanticVersion,
) {
    fun hasUpdate() = when (currentBuildpack.version) {
        is SemanticVersion -> currentBuildpack.version.toSemVer() < latestVersion.toSemVer()
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
