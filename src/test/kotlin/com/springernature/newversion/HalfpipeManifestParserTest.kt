package com.springernature.newversion

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.junit.jupiter.api.Test
import java.io.File

class HalfpipeManifestParserTest {

    @Test
    fun `loads buildpacks from top-level halfpipe file`() {
        val results = HalfpipeManifestParser.load(resourcePath("halfpipe-test")).toList()

        val manifests = results.filterIsInstance<PaketoManifest>()
        val topLevel = manifests.first { it.path.name == ".halfpipe.io" }
        topLevel.buildpacks shouldContainAll listOf(
            VersionedBuildpack.createPaketo("paketobuildpacks/java:21.4.0"),
            VersionedBuildpack.createPaketo("paketobuildpacks/nodejs:1.3.0")
        )
    }

    @Test
    fun `loads buildpacks from nested halfpipe yaml file`() {
        val results = HalfpipeManifestParser.load(resourcePath("halfpipe-test")).toList()

        val manifests = results.filterIsInstance<PaketoManifest>()
        val nested = manifests.first { it.path.name == ".halfpipe.io.yaml" }
        nested.buildpacks shouldContain VersionedBuildpack.createPaketo("paketobuildpacks/java:5.0.0")
    }

    @Test
    fun `finds exactly two manifest files in fixture`() {
        val results = HalfpipeManifestParser.load(resourcePath("halfpipe-test")).toList()

        results.filterIsInstance<PaketoManifest>().size shouldBe 2
        results.filterIsInstance<FailedPaketoManifest>().size shouldBe 0
    }

    @Test
    fun `skips tasks that are not of type buildpack`() {
        val results = HalfpipeManifestParser.load(resourcePath("halfpipe-test")).toList()

        val manifests = results.filterIsInstance<PaketoManifest>()
        val topLevel = manifests.first { it.path.name == ".halfpipe.io" }
        // only 2 buildpacks: the run task should be ignored
        topLevel.buildpacks.size shouldBeEqualTo 2
    }

    @Test
    fun `returns empty sequence for directory with no halfpipe files`() {
        val results = HalfpipeManifestParser.load(resourcePath("all-manifests-test")).toList()

        results.size shouldBe 0
    }

    private fun resourcePath(path: String) = File("src/test/resources/$path")
}
