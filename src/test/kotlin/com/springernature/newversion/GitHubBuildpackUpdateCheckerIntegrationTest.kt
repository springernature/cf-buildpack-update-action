package com.springernature.newversion

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.junit.jupiter.api.Test
import java.net.http.HttpClient

class GitHubBuildpackUpdateCheckerIntegrationTest {

    @Test
    fun `we can query GitHub to find if updates are available`() {
        val buildpackUpdateChecker = GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), Settings())
        val buildpack = VersionedBuildpack(
            "cloudfoundry/java-buildpack",
            "https://github.com/cloudfoundry/java-buildpack",
            SemanticVersion("4.0.20"),
            GitTag("v4.0.20")
        )

        val latestBuildpackVersion = buildpackUpdateChecker.findLatestVersion(buildpack)
        latestBuildpackVersion.version.toSemVer().let {
            it.major.shouldBeGreaterOrEqualTo(4)
            if (it.major == 4) {
                it.minor.shouldBeGreaterOrEqualTo(41)
            }
        }
        latestBuildpackVersion.tag shouldBeEqualTo GitTag("v${latestBuildpackVersion.version}")
    }

}
