package com.springernature.newversion

import java.net.http.HttpClient

fun main() {
    val settings = Settings(System.getenv())
    val client = HttpClient.newBuilder().build()
    val publisher = GitHubPullRequestPublisher(settings)
    val manifesPath = "."

    BuildpackVersionChecker(manifesPath, client, publisher, settings).performChecks()
}
