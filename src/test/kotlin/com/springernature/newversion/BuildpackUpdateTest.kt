package com.springernature.newversion

import org.amshove.kluent.shouldBe
import org.junit.jupiter.api.Test
import java.io.File

class BuildpackUpdateTest {

    @Test
    fun `a newer version is considered an update`() {
        val possibleUpdate = BuildpackUpdate(
            listOf(File("a/path")),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.2.0"), GitTag("v1.2.0")),
            BuildpackVersion(SemanticVersion("1.2.3"), GitTag("v1.2.3"))
        )

        possibleUpdate.hasUpdate() shouldBe false // false positive to test GitHub actions
    }

    @Test
    fun `an older version is not considered an update`() {
        val possibleUpdate = BuildpackUpdate(
            listOf(File("a/path")),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.5.0"), GitTag("v.1.5.0")),
            BuildpackVersion(SemanticVersion("1.2.4"), GitTag("v1.2.4"))
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    @Test
    fun `the same version is not considered an update`() {
        val possibleUpdate = BuildpackUpdate(
            listOf(File("a/path")),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("2.3.4"), GitTag("v2.3.4")),
            BuildpackVersion(SemanticVersion("2.3.4"), GitTag("v2.3.4"))
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    @Test
    fun `a buildpack from HEAD has no updates`() {
        val possibleUpdate = BuildpackUpdate(
            listOf(File("a/path")),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", Unparseable, null),
            BuildpackVersion(SemanticVersion("2.3.4"), GitTag("v2.3.4"))
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    @Test
    fun `semantic versioning is used for upgrade checks`() {
        val possibleUpdate = BuildpackUpdate(
            listOf(File("a/path")),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.3"), GitTag("v1.3")),
            BuildpackVersion(SemanticVersion("1.2.4"), GitTag("v1.2.4"))
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    private class PresetBuildpackUpdateChecker(private val updates: Map<String, BuildpackVersion> = mapOf()) :
        BuildpackUpdateChecker {
        override fun findLatestVersion(buildpack: VersionedBuildpack): BuildpackVersion =
            updates[buildpack.name] ?: BuildpackVersion(SemanticVersion("1.0.0"), GitTag("v1.0.0"))
    }
}