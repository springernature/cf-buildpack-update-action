package com.springernature.newversion

import java.net.http.HttpClient

fun main() {
    val client = HttpClient.newBuilder().build()
    val publisher = GitHubPullRequestPublisher()
    val manifesPath = "."
    val settings = Settings(System.getenv())

    BuildpackVersionChecker(manifesPath, client, publisher, settings).performChecks()
}
