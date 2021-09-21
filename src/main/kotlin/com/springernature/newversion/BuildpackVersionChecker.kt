package com.springernature.newversion

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class BuildpackVersionChecker(
    private val manifestPath: File,
    private val buildpackUpdateChecker: BuildpackUpdateChecker,
    private val publisher: Publisher
) {

    fun performChecks() {
        ManifestParser.load(manifestPath)
            .flatMap { BuildpackUpdate.create(it, buildpackUpdateChecker) }
            .filter(BuildpackUpdate::hasUpdate)
            .forEach {
                try {
                    publisher.publish(it)
                } catch (e: Exception) {
                    LOG.error("Publish of update failed: {}", it, e)
                }
            }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(BuildpackVersionChecker::class.java)
    }

}