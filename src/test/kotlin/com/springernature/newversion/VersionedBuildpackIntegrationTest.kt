package com.springernature.newversion

import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.junit.jupiter.api.Test
import java.net.http.HttpClient

class VersionedBuildpackIntegrationTest {

    @Test
    fun `we can query GitHub to find if updates are available`() {
        val buildpack = VersionedBuildpack(
            "cloudfoundry/java-buildpack",
            "https://github.com/cloudfoundry/java-buildpack",
            SemanticVersion("4.0.20")
        )

        val latestBuildpackVersion = buildpack.findLatestVersion(HttpClient.newBuilder().build(), Settings())
        latestBuildpackVersion.toSemVer().let {
            it.major.shouldBeGreaterOrEqualTo(4)
            if (it.major == 4) {
                it.minor.shouldBeGreaterOrEqualTo(41)
            }
        }
    }

}
