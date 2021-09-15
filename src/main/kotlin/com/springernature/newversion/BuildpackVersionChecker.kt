package com.springernature.newversion

import java.io.File
import java.net.http.HttpClient

class BuildpackVersionChecker(
    private val manifestPath: File,
    private val httpClient: HttpClient,
    private val publisher: Publisher,
    private val settings: Settings
) {

    fun performChecks() {
        loadManifests(manifestPath)
            .flatMap { BuildpackUpdate.create(it, httpClient, settings) }
            .filter(BuildpackUpdate::hasUpdate)
            .forEach(publisher::publish)
    }

}