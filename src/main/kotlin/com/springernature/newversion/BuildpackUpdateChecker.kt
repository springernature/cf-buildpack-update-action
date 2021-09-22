package com.springernature.newversion

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

interface BuildpackUpdateChecker {
    fun findLatestVersion(buildpack: VersionedBuildpack): BuildpackVersion
}

data class BuildpackVersion(val version: SemanticVersion, val tag: GitTag)

@JvmInline
value class GitTag(val value: String)

class GitHubBuildpackUpdateChecker(private val client: HttpClient, private val settings: Settings) :
    BuildpackUpdateChecker {

    override fun findLatestVersion(buildpack: VersionedBuildpack): BuildpackVersion {
        val url = "${settings.lookup(Setting.GIT_HUB_API_URL)}/repos/${buildpack.name}/releases/latest"
        val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw RuntimeException("Unexpected response from GitHub for buildpack $buildpack"
                    + " with URL $url: ${response.statusCode()}; ${response.body()}")
        }
        val message = response.body()
        return Regex(""""tag_name":\s*"(v?([^"]+))"""").find(message)?.let {
            BuildpackVersion(
                SemanticVersion(it.groups[2]!!.value),
                GitTag(it.groups[1]!!.value)
            )
        } ?: throw Exception("Couldn't get latest version of buildpack ${buildpack.name}")
    }

}
