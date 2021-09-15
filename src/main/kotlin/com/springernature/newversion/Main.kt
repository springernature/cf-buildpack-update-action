package com.springernature.newversion

import java.io.File
import java.net.http.HttpClient

fun main() {
    val settings = Settings(System.getenv())
    val client = HttpClient.newBuilder().build()
    val publisher = GitHubPullRequestPublisher(settings)
    val manifestPath = File(".")

    BuildpackVersionChecker(manifestPath, client, publisher, settings).performChecks()
}
