package com.springernature.newversion

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

val gitHubApi: String = System.getenv("GITHUB_API_URL") ?: "https://api.github.com"

data class BuildpackUpdate(
    val manifestPath: String,
    val currentBuildpack: VersionedBuildpack,
    val latestBuildpack: VersionedBuildpack,
)

fun toBuildpackUpdate(it: ManifestLoadResult) = when (it) {
    is FailedManifest -> {
        println(it)
        emptyList()
    }
    is Manifest -> {
        it.applications.flatMap { app -> app.buildpacks }.map { buildpack ->
            BuildpackUpdate(it.path, buildpack, buildpack.getLatestBuildpack())
        }
    }
}

private fun VersionedBuildpack.getLatestBuildpack(): VersionedBuildpack {
    val client = HttpClient.newBuilder().build()
    val url = "$gitHubApi/repos/${name}/releases/latest"
    val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    val message = response.body()
    // "tag_name":"v1.5.24"
    """"tag_name":"v([^"]*)"""".toRegex().find(message)?.let {
        return copy(version = Version(it.groups[1]!!.value))
    }
    throw Exception("Couldn't get latest version of buildpack $name")
}
