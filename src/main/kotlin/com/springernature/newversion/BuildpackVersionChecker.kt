package com.springernature.newversion

import java.net.http.HttpClient

class BuildpackVersionChecker(private val manifestPath: String,
                              private val httpClient: HttpClient,
                              private val publisher: GitHubPullRequestPublisher) {

    fun performChecks() {
        loadManifests(manifestPath)
            .flatMap { BuildpackUpdate.create(it, httpClient) }
            .filter(BuildpackUpdate::hasUpdate)
            .forEach(publisher::publish)
    }

}