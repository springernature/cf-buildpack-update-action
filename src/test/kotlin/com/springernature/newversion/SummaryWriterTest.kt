package com.springernature.newversion

import org.amshove.kluent.fail
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.File

class SummaryWriterTest {

    @Test
    fun `detected workflow buildpack updates appear in report`() {
        val tempFile = getTempReportFile()
        val settings = Settings(
            mapOf(
                Setting.GITHUB_STEP_SUMMARY.key to tempFile.path,
                Setting.GITHUB_STEP_SUMMARY_ENABLED.key to true.toString()
            )
        )
        val results = SuccessfulChecks(emptyList(), listOf(detectedWorkflowUpdate))
        SummaryWriter(settings).write(results)

        tempFile.readText() shouldBeEqualTo """
            |# CF buildpack update action results
            |
            |## detected workflow buildpack updates
            |
            |The following buildpacks were found in GitHub Actions workflow files and have updates available.
            |To enable automatic PR creation, set `UPDATE_WORKFLOW_FILES=true` (requires a GitHub App with the **Workflows** permission — see README).
            |
            |* currentBuildpack VersionedBuildpack(name=test/buildpack1, url=https://a.host/path, version=1.3, tag=GitTag(value=v1.3), fileToken=null) has an update to BuildpackVersion(version=1.4.0, tag=GitTag(value=v1.4.0))
            |
            |Thanks for watching!
            |""".trimMargin()
    }

    @Test
    fun `successful checks report`() {
        val tempFile = getTempReportFile()
        val settings = Settings(
            mapOf(
                Setting.GITHUB_STEP_SUMMARY.key to tempFile.path,
                Setting.GITHUB_STEP_SUMMARY_ENABLED.key to true.toString()
            )
        )
        SummaryWriter(settings).write(successfulChecksResults)

        tempFile.readText() shouldBeEqualTo """
            |# CF buildpack update action results
            |
            |## success
            |
            |* currentBuildpack VersionedBuildpack(name=test/buildpack1, url=https://a.host/path, version=1.3, tag=GitTag(value=v1.3), fileToken=null) has no update
            |
            |Thanks for watching!
            |""".trimMargin()
    }

    @Test
    fun `failed checks report`() {
        val updates: List<BuildpackUpdate> = listOf(buildpackUpdate)
        val errors = mapOf(Pair(buildpackUpdate, RuntimeException("Successful failure!")))
        val results = FailedChecks(updates, errors)

        val tempFile = getTempReportFile()
        val settings = Settings(
            mapOf(
                Setting.GITHUB_STEP_SUMMARY.key to tempFile.path,
                Setting.GITHUB_STEP_SUMMARY_ENABLED.key to true.toString()
            )
        )

        SummaryWriter(settings).write(results)

        tempFile.readText() shouldBeEqualTo """
            |# CF buildpack update action results
            |
            |## failures
            |
            |* VersionedBuildpack(name=test/buildpack1, url=https://a.host/path, version=1.3, tag=GitTag(value=v1.3), fileToken=null) could not be updated: java.lang.RuntimeException: Successful failure!
            |
            |## success
            |
            |* currentBuildpack VersionedBuildpack(name=test/buildpack1, url=https://a.host/path, version=1.3, tag=GitTag(value=v1.3), fileToken=null) has no update
            |
            |Thanks for watching!
            |""".trimMargin()
    }

    @Test
    fun `handle blank filename`() {
        try {
            val settings = Settings(
                mapOf(
                    Setting.GITHUB_STEP_SUMMARY.key to "",
                    Setting.GITHUB_STEP_SUMMARY_ENABLED.key to true.toString()
                )
            )
            SummaryWriter(settings).write(successfulChecksResults)
        } catch (e: Exception) {
            fail("Unexpected exception: $e")
        }
    }

    @Test
    fun `don't write report if disabled`() {
        val tempFile = getTempReportFile()
        val settings = Settings(
            mapOf(
                Setting.GITHUB_STEP_SUMMARY.key to tempFile.path,
                Setting.GITHUB_STEP_SUMMARY_ENABLED.key to false.toString()
            )
        )
        SummaryWriter(settings).write(successfulChecksResults)

        tempFile.readText() shouldBeEqualTo ""
    }


    companion object {
        val buildpackUpdate = BuildpackUpdate(
            listOf(File("a/path")),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.3"), GitTag("v1.3")),
            BuildpackVersion(SemanticVersion("1.2.4"), GitTag("v1.2.4"))
        )

        val detectedWorkflowUpdate = BuildpackUpdate(
            listOf(File("a/path")),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.3"), GitTag("v1.3")),
            BuildpackVersion(SemanticVersion("1.4.0"), GitTag("v1.4.0"))
        )

        val successfulChecksResults = SuccessfulChecks(listOf(buildpackUpdate))

        private fun getTempReportFile(): File {
            val tempFile = kotlin.io.path.createTempFile(suffix = ".md").toFile()
            tempFile.deleteOnExit()
            return tempFile
        }

    }

}