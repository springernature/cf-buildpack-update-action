package com.springernature.newversion

import java.net.http.HttpClient

data class BuildpackUpdate(
    val manifestPath: String,
    val currentBuildpack: VersionedBuildpack,
    val latestBuildpack: VersionedBuildpack,
) {
    fun hasUpdate() = when (currentBuildpack.version) {
        is SemanticVersion -> when (latestBuildpack.version) {
            is SemanticVersion -> currentBuildpack.version.toSemVer() < latestBuildpack.version.toSemVer()
            else -> false
        }
        is Latest -> false
    }

    companion object {
        fun create(manifest: ManifestLoadResult, client: HttpClient, settings: Settings) = when (manifest) {
            is FailedManifest -> {
                println(manifest)
                emptyList()
            }
            is Manifest -> manifest.applications.flatMap { app -> app.buildpacks }.map {
                BuildpackUpdate(manifest.path, it, it.getLatestBuildpack(client, settings))
            }
        }
    }
}
