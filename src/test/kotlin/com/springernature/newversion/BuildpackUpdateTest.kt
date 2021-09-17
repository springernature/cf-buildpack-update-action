package com.springernature.newversion

import org.amshove.kluent.shouldBe
import org.junit.jupiter.api.Test

class BuildpackUpdateTest {

    @Test
    fun `a failed manifest load returns an empty list`() {
        val buildpack =
            BuildpackUpdate.create(FailedManifest("/a/path", RuntimeException("test")), PresetBuildpackUpdateChecker())

        buildpack.isEmpty() shouldBe true
    }

    // TODO successful load

    @Test
    fun `a newer version is considered an update`() {
        val possibleUpdate = BuildpackUpdate(
            "a/path",
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.2.0")),
            SemanticVersion("1.2.3")
        )

        possibleUpdate.hasUpdate() shouldBe true
    }

    @Test
    fun `an older version is not considered an update`() {
        val possibleUpdate = BuildpackUpdate(
            "a/path",
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.5.0")),
            SemanticVersion("1.2.4")
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    @Test
    fun `the same version is not considered an update`() {
        val possibleUpdate = BuildpackUpdate(
            "a/path",
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("2.3.4")),
            SemanticVersion("2.3.4")
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    @Test
    fun `a buildpack from HEAD has no updates`() {
        val possibleUpdate = BuildpackUpdate(
            "a/path",
            VersionedBuildpack("test/buildpack1", "https://a.host/path", Latest()),
            SemanticVersion("2.3.4")
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    @Test
    fun `semantic versioning is used for upgrade checks`() {
        val possibleUpdate = BuildpackUpdate(
            "a/path",
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.3")),
            SemanticVersion("1.2.4")
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    private class PresetBuildpackUpdateChecker : BuildpackUpdateChecker {
        override fun findLatestVersion(buildpack: VersionedBuildpack): SemanticVersion = SemanticVersion("1.0.0")
    }
}