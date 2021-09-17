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
            .forEach {
                try {
                    publisher.publish(it)
                } catch (e: Exception) {
                    System.err.println("Publish of $it failed: ${e.message}")
                    e.printStackTrace(System.err)
                }
            }
    }

}