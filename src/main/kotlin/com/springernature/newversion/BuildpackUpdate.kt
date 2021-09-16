package com.springernature.newversion

import java.net.http.HttpClient

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
        fun create(manifest: ManifestLoadResult, client: HttpClient, settings: Settings) = when (manifest) {
            is FailedManifest -> {
                println(manifest)
                emptyList()
            }
            is Manifest -> manifest.applications.flatMap { app -> app.buildpacks }.map {
                BuildpackUpdate(manifest.path, it, it.findLatestVersion(client, settings))
            }
        }
    }
}
