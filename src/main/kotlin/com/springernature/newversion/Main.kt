package com.springernature.newversion

import java.io.File
import java.net.http.HttpClient
import kotlin.system.exitProcess

fun main() {
    val settings = Settings(System.getenv())
    val httpClient = HttpClient.newBuilder().build()
    val buildpackUpdateChecker = GitHubBuildpackUpdateChecker(httpClient, settings)
    val shellRunner = TurtleShell()
    val publisher = GitHubPullRequestPublisher(shellRunner, settings)
    val manifestPath = File(".")

    var errorDuringChecks = BuildpackVersionChecker(manifestPath, buildpackUpdateChecker, publisher)
        .performChecks()
    if (errorDuringChecks) {
        exitProcess(1)
    }
}
