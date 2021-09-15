package com.springernature.newversion

import java.net.http.HttpClient

fun main() {
    val client = HttpClient.newBuilder().build()
    val publisher = GitHubPullRequestPublisher()
    val manifesPath = "."

    BuildpackVersionChecker(manifesPath, client, publisher).performChecks()
}
