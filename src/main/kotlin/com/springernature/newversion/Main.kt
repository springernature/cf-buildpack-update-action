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

    var results = BuildpackVersionChecker(manifestPath, buildpackUpdateChecker, publisher)
        .performChecks()
    when (results) {
        is FailedChecks -> {
            results.errors.forEach { println("${it.key.currentBuildpack} could not be updated: ${it.value}") }
            exitProcess(1)
        }
        is SuccessfulChecks -> exitProcess(0)
    }

}
