package com.springernature

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

val gitHubApi: String = System.getenv("GITHUB_API_URL") ?: "https://api.github.com"

fun main(args: Array<String>) {
    println("manifest file candidates found:")

    // using extension function walk
    File(".")
        .walk()
        .filterNot { it.path.contains("\\.git/") }
        .filter { it.name.contains("manifest") }
        .filter {
            it.readText(Charsets.UTF_8).contains("buildpacks:")
        }
        .forEach {
            createPR(it)
        }

}

fun createPR(file: File): Unit {
    file.useLines { lines ->
        lines.filter { line -> line.contains("-buildpack#") }
            .forEach { line -> parseBuildpackLine(file, line) }
    }
}

fun parseBuildpackLine(file: File, line: String) {
    // example: "  - https://github.com/cloudfoundry/staticfile-buildpack#v1.5.17"
    ".*github.com/(.*)#v(.*)".toRegex().find(line)?.let {
        val (buildpack, currentVersion) = it.destructured
        println("found buildpack \"$buildpack\" in version \"$currentVersion\"")
        requestLatestVersion(buildpack, currentVersion)
    }
}

fun requestLatestVersion(buildpack: String, currentVersion: String) {
    // $GITHUB_API_URL
    val client = HttpClient.newBuilder().build();
    val url = "$gitHubApi/repos/$buildpack/releases/latest"
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url)).build();

    val response = client.send(request, HttpResponse.BodyHandlers.ofString());
    val message = response.body()
    // "tag_name":"v1.5.24"
    """"tag_name":"v([^"]*)"""".toRegex().find(message)?.let {
        val (latestVersion) = it.destructured
        if (currentVersion == latestVersion) println("$buildpack is up to date")
        else println("could update $buildpack to latest version $latestVersion")
    }

}
