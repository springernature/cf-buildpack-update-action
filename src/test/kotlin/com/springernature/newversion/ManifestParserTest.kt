package com.springernature.newversion

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File

class ManifestParserTest {

    @Test
    fun `loading does not fail on directories`() {
        val loadManifests = ManifestParser.load(resourcePath("/directory-name-test"))
        val failedCount = loadManifests.filterIsInstance<FailedManifest>().count()
        failedCount shouldBe 0
    }

    @Test
    fun `a buildpack can be determined from a manifest with manifest in the name`() {
        val loadManifests = ManifestParser.load(resourcePath("manifest.yml"))

        var parsedManifests = 0
        loadManifests
            .onEach { parsedManifests++ }
            .forEach {
                when (it) {
                    is FailedManifest -> fail("Manifest load failed: $it")
                    is Manifest -> {
                        it.applications.size shouldBe 1
                        it.applications[0].buildpacks() shouldContain VersionedBuildpack(
                            "cloudfoundry/staticfile-buildpack",
                            "https://github.com/cloudfoundry/staticfile-buildpack",
                            SemanticVersion("1.5.17"),
                            GitTag("v1.5.17")
                        )
                    }
                }
            }

        parsedManifests shouldBe 1
    }

    @Test
    fun `a buildpack can be determined from a manifest with cf in the name`() {
        val loadManifests = ManifestParser.load(resourcePath("cf.yml"))

        var parsedManifests = 0
        loadManifests
            .onEach { parsedManifests++ }
            .forEach {
                when (it) {
                    is FailedManifest -> fail("Manifest load failed: $it")
                    is Manifest -> {
                        it.applications.size shouldBe 1
                        it.applications[0].buildpacks() shouldContain VersionedBuildpack(
                            "cloudfoundry/staticfile-buildpack",
                            "https://github.com/cloudfoundry/staticfile-buildpack",
                            SemanticVersion("1.5.17"),
                            GitTag("v1.5.17")
                        )
                    }
                }
            }

        parsedManifests shouldBe 1
    }

    @Test
    fun `a buildpack with a git suffix can be determined from a manifest`() {
        val loadManifests = ManifestParser.load(resourcePath("manifest-git-suffix.yml"))

        var parsedManifests = 0
        loadManifests
            .onEach { parsedManifests++ }
            .forEach {
                when (it) {
                    is FailedManifest -> fail("Manifest load failed: $it")
                    is Manifest -> {
                        it.applications.size shouldBe 1
                        it.applications[0].buildpacks() shouldContain VersionedBuildpack(
                            "cloudfoundry/ruby-buildpack",
                            "https://github.com/cloudfoundry/ruby-buildpack",
                            SemanticVersion("1.8.46"),
                            GitTag("v1.8.46")
                        )
                    }
                }
            }

        parsedManifests shouldBe 1
    }

    @Test
    fun `all buildpacks in a manifest are loaded`() {
        val loadManifests = ManifestParser.load(resourcePath("manifest-with-multiple-buildpacks.yml"))

        var parsedManifests = 0
        loadManifests
            .onEach { parsedManifests++ }
            .forEach {
                when (it) {
                    is FailedManifest -> fail("Manifest load failed: $it")
                    is Manifest -> {
                        it.applications.size shouldBe 1
                        it.applications[0].buildpacks().let { buildpacks ->
                            buildpacks.size shouldBe 2
                            buildpacks shouldContainAll listOf(
                                VersionedBuildpack(
                                    "cloudfoundry/staticfile-buildpack",
                                    "https://github.com/cloudfoundry/staticfile-buildpack",
                                    SemanticVersion("1.5.17"),
                                    GitTag("v1.5.17")
                                ),
                                VersionedBuildpack(
                                    "cloudfoundry/java-buildpack",
                                    "https://github.com/cloudfoundry/java-buildpack",
                                    SemanticVersion("4.12"),
                                    GitTag("v4.12")
                                )
                            )
                        }
                    }
                }
            }

        parsedManifests shouldBe 1
    }

    @Test
    fun `a buildpack without a version is parsed correctly`() {
        val loadManifests = ManifestParser.load(resourcePath("manifest-without-version.yml"))

        var parsedManifests = 0
        loadManifests
            .onEach { parsedManifests++ }
            .forEach {
                when (it) {
                    is FailedManifest -> fail("Manifest load failed: $it")
                    is Manifest -> {
                        it.applications.size shouldBe 1
                        it.applications[0].buildpacks().let { buildpacks ->
                            buildpacks.size shouldBe 1
                            buildpacks shouldContain VersionedBuildpack(
                                "springernature/a-custom-buildpack",
                                "https://github.com/springernature/a-custom-buildpack",
                                Unparseable,
                                null
                            )
                        }
                    }
                }
            }

        parsedManifests shouldBe 1
    }

    @Test
    fun `a builtin buildpack is parsed correctly`() {
        val loadManifests = ManifestParser.load(resourcePath("manifest-with-builtin-buildpack.yml"))

        var parsedManifests = 0
        loadManifests
            .onEach { parsedManifests++ }
            .forEach {
                when (it) {
                    is FailedManifest -> fail("Manifest load failed: $it")
                    is Manifest -> {
                        it.applications.size shouldBe 1
                        it.applications[0].buildpacks().let { buildpacks ->
                            buildpacks.size shouldBe 1
                            buildpacks shouldContain VersionedBuildpack(
                                "cloudfoundry/a-builtin-buildpack",
                                "https://github.com/cloudfoundry/a-builtin-buildpack",
                                Unparseable,
                                null
                            )
                        }
                    }
                }
            }

        parsedManifests shouldBe 1
    }

    @Test
    fun `a manifest with a legacy buildpack attribute is parsed correctly`() {
        val loadManifests = ManifestParser.load(resourcePath("manifest-legacy.yml"))

        var parsedManifests = 0
        loadManifests
            .onEach { parsedManifests++ }
            .forEach {
                when (it) {
                    is FailedManifest -> fail("Manifest load failed: $it")
                    is Manifest -> {
                        it.applications.size shouldBe 1
                        it.applications[0].buildpacks().let { buildpacks ->
                            buildpacks.size shouldBe 1
                            buildpacks shouldContain VersionedBuildpack(
                                "cloudfoundry/staticfile-buildpack",
                                "https://github.com/cloudfoundry/staticfile-buildpack",
                                SemanticVersion("1.5.17"),
                                GitTag("v1.5.17")
                            )
                        }
                    }
                }
            }

        parsedManifests shouldBe 1
    }

    @Test
    fun `an empty manifest is handled gracefully`() {
        val loadManifests = ManifestParser.load(resourcePath("manifest-empty.yml"))

        val failedCount = loadManifests.filterIsInstance<FailedManifest>().count()
        failedCount shouldBe 1
        loadManifests.count() shouldBe 1
    }


    private fun resourcePath(path: String) = File("src/test/resources/$path")

}

