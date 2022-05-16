package com.springernature.newversion

import java.io.File

class SummaryWriter(val file: File) {

    constructor(filename: String?) : this(
        if (filename.isNullOrBlank()) {
            System.err.println("no filename for report provided, will not create a report")
            File("/dev/null")
        } else {
            File(filename)
        }
    )

    fun write(results: ChecksResult) {
        writeHeader(file)
        when (results) {
            is FailedChecks -> {
                writeFailures(results.errors)
                writeSuccessfulUpdates(results.updates)
            }
            is SuccessfulChecks -> writeSuccessfulUpdates(results.updates)
        }
        writeFooter(file)
    }

    private fun writeHeader(file: File) {
        file.writeText("# CF buildpack update action results\n")
    }

    private fun writeSuccessfulUpdates(updates: List<BuildpackUpdate>) {
        if (updates.isEmpty()) {
            return
        }

        file.appendText("\n## success\n\n")
        updates.forEach { file.appendText("* currentBuildpack ${it.currentBuildpack} ${if (it.hasUpdate()) "has an update to " + it.latestUpdate else "has no update"}\n") }
    }

    private fun writeFailures(errors: Map<BuildpackUpdate, Exception>) {
        if (errors.isEmpty()) {
            return
        }

        file.appendText(
            "\n## failures\n" +
                    "\n"
        )
        errors.forEach { file.appendText("* ${it.key.currentBuildpack} could not be updated: ${it.value}\n") }
    }

    private fun writeFooter(file: File) {
        file.appendText("\nThanks for watching!\n")
    }

}
