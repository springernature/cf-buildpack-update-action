package com.springernature.newversion

import java.io.File
import java.net.http.HttpClient

fun main() {
    val settings = Settings(System.getenv())
    val httpClient = HttpClient.newBuilder().build()
    val buildpackUpdateChecker = GitHubBuildpackUpdateChecker(httpClient, settings)
    val shellRunner = TurtleShell()
    val publisher = GitHubPullRequestPublisher(shellRunner, settings)
    val manifestPath = File(".")

    BuildpackVersionChecker(manifestPath, buildpackUpdateChecker, publisher)
        .performChecks()
}
