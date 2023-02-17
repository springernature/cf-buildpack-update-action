package com.springernature.newversion

import org.amshove.kluent.fail
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.File

class SummaryWriterTest {

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
            |* currentBuildpack VersionedBuildpack(name=test/buildpack1, url=https://a.host/path, version=1.3, tag=GitTag(value=v1.3)) has no update
            |
            |Thanks for watching!
            |""".trimMargin()
    }

    @Test
    fun `failed checks report`() {
        val updates: List<BuildpackUpdate> = listOf(buildpackUpdate)
        val errors = mapOf(Pair(buildpackUpdate, FailureResult(buildpackUpdate, "Successful failure!")))
        val results = FailedChecks(updates.map { SuccessResult(it) }, errors)

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
            |* VersionedBuildpack(name=test/buildpack1, url=https://a.host/path, version=1.3, tag=GitTag(value=v1.3)) could not be updated: FailureResult(update=BuildpackUpdate(manifests=[a/path], currentBuildpack=VersionedBuildpack(name=test/buildpack1, url=https://a.host/path, version=1.3, tag=GitTag(value=v1.3)), latestUpdate=BuildpackVersion(version=1.2.4, tag=GitTag(value=v1.2.4))), reason=Successful failure!)
            |
            |## success
            |
            |* currentBuildpack VersionedBuildpack(name=test/buildpack1, url=https://a.host/path, version=1.3, tag=GitTag(value=v1.3)) has no update
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

        val successfulChecksResults = SuccessfulChecks(listOf(SuccessResult(buildpackUpdate)), emptyList())

        private fun getTempReportFile(): File {
            val tempFile = kotlin.io.path.createTempFile(suffix = ".md").toFile()
            tempFile.deleteOnExit()
            return tempFile
        }

    }

}