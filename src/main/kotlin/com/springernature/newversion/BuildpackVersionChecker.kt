package com.springernature.newversion

import java.io.File

class BuildpackVersionChecker(
    private val manifestPath: File,
    private val buildpackUpdateChecker: BuildpackUpdateChecker,
    private val publisher: Publisher
) {

    fun performChecks() {
        loadManifests(manifestPath)
            .flatMap { BuildpackUpdate.create(it, buildpackUpdateChecker) }
            .filter(BuildpackUpdate::hasUpdate)
            .forEach(publisher::publish)
    }

}