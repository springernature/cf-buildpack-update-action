package com.springernature.newversion

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class VersionedBuildpackTest {

    @Test
    fun `createPaketo parses org, repo and version from paketobuildpacks token`() {
        val buildpack = VersionedBuildpack.createPaketo("paketobuildpacks/java:21.4.0")

        buildpack.name shouldBeEqualTo "paketo-buildpacks/java"
        buildpack.url shouldBeEqualTo "https://github.com/paketo-buildpacks/java"
        buildpack.version shouldBeEqualTo SemanticVersion("21.4.0")
        buildpack.tag shouldBe null
        buildpack.fileToken shouldBeEqualTo "paketobuildpacks/java"
    }

    @Test
    fun `createPaketo returns Unparseable version when token has no version`() {
        val buildpack = VersionedBuildpack.createPaketo("paketobuildpacks/java")

        buildpack.version shouldBe Unparseable
    }

    @Test
    fun `existing CF buildpacks have null fileToken by default`() {
        val buildpack = VersionedBuildpack.create(
            "https://github.com/cloudfoundry/staticfile-buildpack#v1.5.17"
        )

        buildpack.fileToken shouldBe null
    }
}
