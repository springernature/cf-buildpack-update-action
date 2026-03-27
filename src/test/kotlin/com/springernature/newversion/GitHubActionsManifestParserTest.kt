package com.springernature.newversion

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.junit.jupiter.api.Test
import java.io.File

class GitHubActionsManifestParserTest {

    @Test
    fun `loads buildpack from top-level workflow file`() {
        val results = GitHubActionsManifestParser.load(resourcePath("github-actions-test")).toList()

        val manifests = results.filterIsInstance<PaketoManifest>()
        val topLevel = manifests.first { it.path.invariantSeparatorsPath.contains("/github-actions-test/.github/workflows/") }
        topLevel.buildpacks shouldContain VersionedBuildpack.createPaketo("paketobuildpacks/java:21.4.0")
    }

    @Test
    fun `loads buildpack from nested workflow file`() {
        val results = GitHubActionsManifestParser.load(resourcePath("github-actions-test")).toList()

        val manifests = results.filterIsInstance<PaketoManifest>()
        val nested = manifests.first { it.path.invariantSeparatorsPath.contains("/subapp/.github/workflows/") }
        nested.buildpacks shouldContain VersionedBuildpack.createPaketo("paketobuildpacks/nodejs:1.3.0")
    }

    @Test
    fun `finds exactly two workflow files in fixture`() {
        val results = GitHubActionsManifestParser.load(resourcePath("github-actions-test")).toList()

        results.filterIsInstance<PaketoManifest>().size shouldBe 2
        results.filterIsInstance<FailedPaketoManifest>().size shouldBe 0
    }

    @Test
    fun `returns empty sequence for directory with no workflow files`() {
        val results = GitHubActionsManifestParser.load(resourcePath("all-manifests-test")).toList()

        results.size shouldBe 0
    }

    @Test
    fun `steps without buildpacks key are ignored`() {
        val results = GitHubActionsManifestParser.load(resourcePath("github-actions-test")).toList()

        val manifests = results.filterIsInstance<PaketoManifest>()
        val nested = manifests.first { it.path.invariantSeparatorsPath.contains("/subapp/.github/workflows/") }
        // Only one buildpack in that file; the setup-node step has no buildpacks
        nested.buildpacks.size shouldBe 1
    }

    private fun resourcePath(path: String) = File("src/test/resources/$path")
}
