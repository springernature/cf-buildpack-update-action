package com.springernature.newversion

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContainAll
import org.junit.jupiter.api.Test
import java.io.File

class BuildpackUpdateTest {

    @Test
    fun `a failed manifest load returns an empty list`() {
        val buildpacks =
            BuildpackUpdate.create(FailedManifest(File("/a/path"), RuntimeException("test")), PresetBuildpackUpdateChecker())

        buildpacks.isEmpty() shouldBe true
    }

    @Test
    fun `a successful manifest load returns a list of the specified buildpacks`() {
        val buildpack1 = VersionedBuildpack("test/one", "https://host/path/1", SemanticVersion("1.2.3"), GitTag("v1.2.3"))
        val buildpack2 = VersionedBuildpack("test/two", "https://host/path/2", SemanticVersion("4.5.6"), GitTag("v4.5.6"))
        val buildpack3 = VersionedBuildpack("test/three", "https://host/path/3", Latest, null)

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
            BuildpackUpdate(File("."), buildpack1, BuildpackVersion(SemanticVersion("1.0.0"), GitTag("v1.0.0"))),
            BuildpackUpdate(File("."), buildpack2, BuildpackVersion(SemanticVersion("1.0.0"), GitTag("v1.0.0"))),
            BuildpackUpdate(File("."), buildpack3, BuildpackVersion(SemanticVersion("1.0.0"), GitTag("v1.0.0")))
        )
    }

    @Test
    fun `a successful manifest load checks for buildpack updates`() {
        val buildpack1 = VersionedBuildpack("test/one", "https://host/path/1", SemanticVersion("1.2.3"), GitTag("v1.2.3"))
        val buildpack2 = VersionedBuildpack("test/two", "https://host/path/2", SemanticVersion("4.5.6"), GitTag("v4.5.6"))

        val buildpacks = BuildpackUpdate.create(
            Manifest(
                listOf(
                    CFApplication(listOf(buildpack1, buildpack2))
                )
            ),
            PresetBuildpackUpdateChecker(
                mapOf(
                    "test/one" to BuildpackVersion(SemanticVersion("2.3.4"), GitTag("v2.3.4")),
                    "test/two" to BuildpackVersion(SemanticVersion("5.6.7"), GitTag("v5.6.7"))
                )
            )
        )

        buildpacks shouldContainAll listOf(
            BuildpackUpdate(File("."), buildpack1, BuildpackVersion(SemanticVersion("2.3.4"), GitTag("v2.3.4"))),
            BuildpackUpdate(File("."), buildpack2, BuildpackVersion(SemanticVersion("5.6.7"), GitTag("v5.6.7"))),
        )
    }

    @Test
    fun `a newer version is considered an update`() {
        val possibleUpdate = BuildpackUpdate(
            File("a/path"),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.2.0"), GitTag("v1.2.0")),
            BuildpackVersion(SemanticVersion("1.2.3"), GitTag("v1.2.3"))
        )

        possibleUpdate.hasUpdate() shouldBe true
    }

    @Test
    fun `an older version is not considered an update`() {
        val possibleUpdate = BuildpackUpdate(
            File("a/path"),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.5.0"), GitTag("v.1.5.0")),
            BuildpackVersion(SemanticVersion("1.2.4"), GitTag("v1.2.4"))
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    @Test
    fun `the same version is not considered an update`() {
        val possibleUpdate = BuildpackUpdate(
            File("a/path"),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("2.3.4"), GitTag("v2.3.4")),
            BuildpackVersion(SemanticVersion("2.3.4"), GitTag("v2.3.4"))
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    @Test
    fun `a buildpack from HEAD has no updates`() {
        val possibleUpdate = BuildpackUpdate(
            File("a/path"),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", Latest, null),
            BuildpackVersion(SemanticVersion("2.3.4"), GitTag("v2.3.4"))
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    @Test
    fun `semantic versioning is used for upgrade checks`() {
        val possibleUpdate = BuildpackUpdate(
            File("a/path"),
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.3"), GitTag("v1.3")),
            BuildpackVersion(SemanticVersion("1.2.4"), GitTag("v1.2.4"))
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    private class PresetBuildpackUpdateChecker(private val updates: Map<String, BuildpackVersion> = mapOf())
        : BuildpackUpdateChecker {
        override fun findLatestVersion(buildpack: VersionedBuildpack): BuildpackVersion =
            updates[buildpack.name] ?: BuildpackVersion(SemanticVersion("1.0.0"), GitTag("v1.0.0"))
    }
}