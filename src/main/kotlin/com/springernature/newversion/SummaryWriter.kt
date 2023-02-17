package com.springernature.newversion

import java.io.File

class SummaryWriter(val settings: Settings) {

    val file = if (settings.lookup(Setting.GITHUB_STEP_SUMMARY).isNotBlank()) {
        File(settings.lookup(Setting.GITHUB_STEP_SUMMARY))
    } else {
        File("/dev/null")
    }

    fun write(results: ChecksResult) {
        if (!settings.lookup(Setting.GITHUB_STEP_SUMMARY_ENABLED).toBoolean()) {
            return
        }
        writeHeader(file)
        when (results) {
            is FailedChecks -> {
                writeFailures(results.errors)
                writeSuccessfulUpdates(results.updates.filterIsInstance<SuccessResult>())
            }
            is SuccessfulChecks -> writeSuccessfulUpdates(results.updates)
        }
        writeFooter(file)
    }

    private fun writeHeader(file: File) {
        file.writeText("# CF buildpack update action results\n")
    }

    private fun writeSuccessfulUpdates(updates: List<SuccessResult>) {
        if (updates.isEmpty()) {
            return
        }

        file.appendText("\n## success\n\n")
        updates.forEach {
            file.appendText("* currentBuildpack ${it.update.currentBuildpack} ")
            file.appendText(if (it.update.hasUpdate()) "has an update to " + it.update.latestUpdate else "has no update")
            file.appendText("\n")
        }
    }

    private fun writeFailures(errors: Map<BuildpackUpdate, FailureResult>) {
        if (errors.isEmpty()) {
            return
        }

        file.appendText(
            "\n## failures\n" + "\n"
        )
        errors.forEach { file.appendText("* ${it.key.currentBuildpack} could not be updated: ${it.value}\n") }
    }

    private fun writeFooter(file: File) {
        file.appendText("\nThanks for watching!\n")
    }

}
