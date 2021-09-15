package com.springernature.newversion

import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import kotlin.test.fail

class VersionedBuildpackIntegrationTest {

    @Test
    fun `we can query GitHub to find if updates are available`() {
        val buildpack = VersionedBuildpack(
            "cloudfoundry/java-buildpack",
            "https://github.com/cloudfoundry/java-buildpack",
            SemanticVersion("4.0.20")
        )

        val latestBuildpack = buildpack.getLatestBuildpack(HttpClient.newBuilder().build(), Settings())

        when (val latestVersion = latestBuildpack.version) {
            is Latest -> fail("Version lookup failed")
            is SemanticVersion -> {
                val latestSemVer = latestVersion.toSemVer()
                latestSemVer.major.shouldBeGreaterOrEqualTo(4)
                if (latestSemVer.major == 4) {
                    latestSemVer.minor.shouldBeGreaterOrEqualTo(41)
                }
            }
        }
    }

}
