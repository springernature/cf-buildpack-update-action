package com.springernature.newversion

import org.amshove.kluent.fail
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.File

class SummaryWriterTest {

    @Test
    fun `successful checks report`() {
        val tempFile = getTempReportFile()

        SummaryWriter(tempFile).write(successfulChecksResults)

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
        val errors = mapOf(Pair(buildpackUpdate, RuntimeException("Successful failure!")))
        val results = FailedChecks(updates, errors)

        val tempFile = getTempReportFile()

        SummaryWriter(tempFile).write(results)

        tempFile.readText() shouldBeEqualTo """
            |# CF buildpack update action results
            |
            |## failures
            |
            |* VersionedBuildpack(name=test/buildpack1, url=https://a.host/path, version=1.3, tag=GitTag(value=v1.3)) could not be updated: java.lang.RuntimeException: Successful failure!
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
            SummaryWriter("").write(successfulChecksResults)
        } catch (e: Exception) {
            fail("Unexpected exception: $e")
        }
    }

    @Test
    fun `handle null filename`() {
        try {
            SummaryWriter(null).write(successfulChecksResults)
        } catch (e: Exception) {
            fail("Unexpected exception: $e")
        }
    }

    companion object {
        val buildpackUpdate = BuildpackUpdate(
            listOf(File("a/path")),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.3"), GitTag("v1.3")),
            BuildpackVersion(SemanticVersion("1.2.4"), GitTag("v1.2.4"))
        )

        val successfulChecksResults = SuccessfulChecks(listOf(buildpackUpdate))

        private fun getTempReportFile(): File {
            val tempFile = kotlin.io.path.createTempFile(suffix = ".md").toFile()
            tempFile.deleteOnExit()
            return tempFile
        }

    }

}