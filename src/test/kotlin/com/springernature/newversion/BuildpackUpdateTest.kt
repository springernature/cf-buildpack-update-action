package com.springernature.newversion

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContainAll
import org.junit.jupiter.api.Test

class BuildpackUpdateTest {

    @Test
    fun `a failed manifest load returns an empty list`() {
        val buildpacks =
            BuildpackUpdate.create(FailedManifest("/a/path", RuntimeException("test")), PresetBuildpackUpdateChecker())

        buildpacks.isEmpty() shouldBe true
    }

    @Test
    fun `a successful manifest load returns a list of the specified buildpacks`() {
        val buildpack1 = VersionedBuildpack("test/one", "https://host/path/1", SemanticVersion("1.2.3"))
        val buildpack2 = VersionedBuildpack("test/two", "https://host/path/2", SemanticVersion("4.5.6"))
        val buildpack3 = VersionedBuildpack("test/three", "https://host/path/3", Latest())

        val buildpacks = BuildpackUpdate.create(
            Manifest(
                listOf(
                    CFApplication(listOf(buildpack1, buildpack2)),
                    CFApplication(listOf(buildpack3))
                )
            ),
            PresetBuildpackUpdateChecker()
        )

        buildpacks shouldContainAll listOf(
            BuildpackUpdate("", buildpack1, SemanticVersion("1.0.0")),
            BuildpackUpdate("", buildpack2, SemanticVersion("1.0.0")),
            BuildpackUpdate("", buildpack3, SemanticVersion("1.0.0"))
        )
    }

    @Test
    fun `a successful manifest load checks for buildpack updates`() {
        val buildpack1 = VersionedBuildpack("test/one", "https://host/path/1", SemanticVersion("1.2.3"))
        val buildpack2 = VersionedBuildpack("test/two", "https://host/path/2", SemanticVersion("4.5.6"))

        val buildpacks = BuildpackUpdate.create(
            Manifest(
                listOf(
                    CFApplication(listOf(buildpack1, buildpack2))
                )
            ),
            PresetBuildpackUpdateChecker(
                mapOf(
                    "test/one" to SemanticVersion("2.3.4"),
                    "test/two" to SemanticVersion("5.6.7")
                )
            )
        )

        buildpacks shouldContainAll listOf(
            BuildpackUpdate("", buildpack1, SemanticVersion("2.3.4")),
            BuildpackUpdate("", buildpack2, SemanticVersion("5.6.7")),
        )
    }

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

    private class PresetBuildpackUpdateChecker(private val updates: Map<String, SemanticVersion> = mapOf())
        : BuildpackUpdateChecker {
        override fun findLatestVersion(buildpack: VersionedBuildpack): SemanticVersion =
            updates[buildpack.name] ?: SemanticVersion("1.0.0")
    }
}