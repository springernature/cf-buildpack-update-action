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
        val loadManifests = loadManifests(resourcePath("/directory-name-test"))
        val failedCount = loadManifests.filterIsInstance<FailedManifest>().count()
        failedCount shouldBe 0
    }

    @Test
    fun `a buildpack can be determined from a manifest`() {
        val loadManifests = loadManifests(resourcePath("manifest.yml"))

        var parsedManifests = 0
        loadManifests
            .also { parsedManifests++ }
            .forEach {
                when (it) {
                    is FailedManifest -> fail("Manifest load failed: $it")
                    is Manifest -> {
                        it.applications.size shouldBe 1
                        it.applications[0].buildpacks shouldContain VersionedBuildpack(
                            "cloudfoundry/staticfile-buildpack",
                            "https://github.com/cloudfoundry/staticfile-buildpack",
                            SemanticVersion("1.5.17")
                        )
                    }
                }
            }

        parsedManifests shouldBe 1
    }

    @Test
    fun `all buildpacks in a manifest are loaded`() {
        val loadManifests = loadManifests(resourcePath("manifest-with-multiple-buildpacks.yml"))

        var parsedManifests = 0
        loadManifests
            .also { parsedManifests++ }
            .forEach {
                when (it) {
                    is FailedManifest -> fail("Manifest load failed: $it")
                    is Manifest -> {
                        it.applications.size shouldBe 1
                        it.applications[0].buildpacks.let { buildpacks ->
                            buildpacks.size shouldBe 2
                            buildpacks shouldContainAll listOf(
                                VersionedBuildpack(
                                    "cloudfoundry/staticfile-buildpack",
                                    "https://github.com/cloudfoundry/staticfile-buildpack",
                                    SemanticVersion("1.5.17")
                                ),
                                VersionedBuildpack(
                                    "cloudfoundry/java-buildpack",
                                    "https://github.com/cloudfoundry/java-buildpack",
                                    SemanticVersion("4.12")
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
        val loadManifests = loadManifests(resourcePath("manifest-without-version.yml"))

        var parsedManifests = 0
        loadManifests
            .also { parsedManifests++ }
            .forEach {
                when (it) {
                    is FailedManifest -> fail("Manifest load failed: $it")
                    is Manifest -> {
                        it.applications.size shouldBe 1
                        it.applications[0].buildpacks.let { buildpacks ->
                            buildpacks.size shouldBe 1
                            buildpacks shouldContain VersionedBuildpack(
                                "springernature/nginx-opentracing-buildpack",
                                "https://github.com/springernature/nginx-opentracing-buildpack",
                                Latest()
                            )
                        }
                    }
                }
            }

        parsedManifests shouldBe 1
    }

    private fun resourcePath(path: String): String = File("src/test/resources/").absolutePath + "/" + path

}

