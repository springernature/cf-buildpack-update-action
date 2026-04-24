package com.springernature.newversion

import com.springernature.newversion.Setting.GIT_HUB_API_URL
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.net.InetSocketAddress
import java.net.http.HttpClient

class BuildpackVersionCheckerTest {

    @Test
    fun `we query GitHub to find if updates are available`() {
        val manifest = File("src/test/resources/manifest.yml")
        manifest.exists() shouldBe true

        val settings = Settings(mapOf(GIT_HUB_API_URL.key to baseUrl))
        val capturingPublisher = CapturingPublisher()
        val buildpackVersionChecker = BuildpackVersionChecker(
            manifest,
            GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings),
            capturingPublisher
        )

        val results = buildpackVersionChecker.performChecks()
        results shouldBeInstanceOf SuccessfulChecks::class

        capturingPublisher.updates().size shouldBe 1
        val lastUpdate = capturingPublisher.updates().first()

        lastUpdate.currentBuildpack.name shouldBeEqualTo "cloudfoundry/staticfile-buildpack"
        lastUpdate.currentBuildpack.version shouldBeEqualTo SemanticVersion("1.5.17")
        lastUpdate.latestUpdate.version shouldBeEqualTo SemanticVersion("1.5.24")
        lastUpdate.latestUpdate.tag shouldBeEqualTo GitTag("v1.5.24")
    }

    @Test
    fun `we don't query GitHub to find if updates are available for buildpacks on head`() {
        val manifest = File("src/test/resources/manifest-without-version.yml")
        manifest.exists() shouldBe true

        val settings = Settings(mapOf(GIT_HUB_API_URL.key to baseUrl))
        val capturingPublisher = CapturingPublisher()
        val buildpackVersionChecker = BuildpackVersionChecker(
            manifest,
            GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings),
            capturingPublisher
        )

        val results = buildpackVersionChecker.performChecks()
        results shouldBeInstanceOf SuccessfulChecks::class

        capturingPublisher.updates().size shouldBe 0
    }

    @Test
    fun `we don't query GitHub to find if updates are available for buildpacks with non-semver branches or tags`() {
        val manifest = File("src/test/resources/manifest-without-semver-tag.yml")
        manifest.exists() shouldBe true

        val settings = Settings(mapOf(GIT_HUB_API_URL.key to baseUrl))
        val capturingPublisher = CapturingPublisher()
        val buildpackVersionChecker = BuildpackVersionChecker(
            manifest,
            GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings),
            capturingPublisher
        )

        val results = buildpackVersionChecker.performChecks()
        results shouldBeInstanceOf SuccessfulChecks::class

        capturingPublisher.updates().size shouldBe 0
    }

    @Test
    fun `we can find the latest version if version only specifies major and minor but no patch version`() {
        val manifest = File("src/test/resources/manifest-no-patch-version-specified.yml")
        manifest.exists() shouldBe true

        val settings = Settings(mapOf(GIT_HUB_API_URL.key to baseUrl))
        val capturingPublisher = CapturingPublisher()
        val buildpackVersionChecker = BuildpackVersionChecker(
            manifest,
            GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings),
            capturingPublisher
        )

        val results = buildpackVersionChecker.performChecks()
        results shouldBeInstanceOf SuccessfulChecks::class

        capturingPublisher.updates().size shouldBe 1
        val lastUpdate = capturingPublisher.updates().first()

        lastUpdate.currentBuildpack.name shouldBeEqualTo "cloudfoundry/java-buildpack"
        lastUpdate.currentBuildpack.version shouldBeEqualTo SemanticVersion("4.20")
        lastUpdate.latestUpdate.version shouldBeEqualTo SemanticVersion("4.41")
        lastUpdate.latestUpdate.tag shouldBeEqualTo GitTag("v4.41")
    }

    @Test
    fun `a failed manifest load performs no updates`() {
        val manifest = File("src/test/resources/manifest-bad.yml")
        manifest.exists() shouldBe true

        val settings = Settings(mapOf(GIT_HUB_API_URL.key to baseUrl))
        val capturingPublisher = CapturingPublisher()
        val buildpackVersionChecker = BuildpackVersionChecker(
            manifest,
            GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings),
            capturingPublisher
        )

        val results = buildpackVersionChecker.performChecks()
        results shouldBeInstanceOf SuccessfulChecks::class

        capturingPublisher.updates().size shouldBe 0
    }

    @Test
    fun `a successful manifest load acts on every loaded buildpack`() {
        val manifestDir = File("src/test/resources/all-manifests-test")
        manifestDir.exists() shouldBe true
        manifestDir.listFiles()?.size shouldBe 3

        val settings = Settings(mapOf(GIT_HUB_API_URL.key to baseUrl))
        val capturingPublisher = CapturingPublisher()
        val buildpackVersionChecker = BuildpackVersionChecker(
            manifestDir,
            GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings),
            capturingPublisher
        )

        val results = buildpackVersionChecker.performChecks()
        results shouldBeInstanceOf SuccessfulChecks::class

        val updates = capturingPublisher.updates().sortedBy { it.currentBuildpack.name }
        updates.size shouldBe 2

        updates[0].let {
            it.manifests shouldContainSame listOf(
                File(manifestDir, "manifest2.yml"),
                File(manifestDir, "manifest3.yml")
            )
            it.currentBuildpack.name shouldBeEqualTo "cloudfoundry/java-buildpack"
            it.currentBuildpack.version shouldBeEqualTo SemanticVersion("4.39")
            it.latestUpdate.version shouldBeEqualTo SemanticVersion("4.41")
            it.latestUpdate.tag shouldBeEqualTo GitTag("v4.41")
        }

        updates[1].let {
            it.manifests shouldContainSame listOf(File(manifestDir, "manifest1.yml"))
            it.currentBuildpack.name shouldBeEqualTo "cloudfoundry/staticfile-buildpack"
            it.currentBuildpack.version shouldBeEqualTo SemanticVersion("1.5.17")
            it.latestUpdate.version shouldBeEqualTo SemanticVersion("1.5.24")
            it.latestUpdate.tag shouldBeEqualTo GitTag("v1.5.24")
        }
    }

    @Test
    fun `paketo buildpacks in halfpipe manifests are checked and updated`() {
        val fixtureDir = File("src/test/resources/paketo-wiring-test")
        fixtureDir.exists() shouldBe true

        val settings = Settings(mapOf(GIT_HUB_API_URL.key to baseUrl))
        val capturingPublisher = CapturingPublisher()
        val buildpackVersionChecker = BuildpackVersionChecker(
            fixtureDir,
            GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings),
            capturingPublisher
        )

        val results = buildpackVersionChecker.performChecks()
        results shouldBeInstanceOf SuccessfulChecks::class

        capturingPublisher.updates().size shouldBe 1
        val update = capturingPublisher.updates().first()

        update.currentBuildpack.name shouldBeEqualTo "paketo-buildpacks/java"
        update.currentBuildpack.version shouldBeEqualTo SemanticVersion("21.4.0")
        update.latestUpdate.version shouldBeEqualTo SemanticVersion("21.5.0")
        update.latestUpdate.tag shouldBeEqualTo GitTag("v21.5.0")
    }

    @Test
    fun `paketo buildpacks in github actions workflows are detected but not published by default`() {
        val fixtureDir = File("src/test/resources/github-actions-test")
        fixtureDir.exists() shouldBe true

        val settings = Settings(mapOf(GIT_HUB_API_URL.key to baseUrl))
        val capturingPublisher = CapturingPublisher()
        val buildpackVersionChecker = BuildpackVersionChecker(
            fixtureDir,
            GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings),
            capturingPublisher,
            settings
        )

        val results = buildpackVersionChecker.performChecks()
        results shouldBeInstanceOf SuccessfulChecks::class

        capturingPublisher.updates().size shouldBe 0

        val detected = results.detectedWorkflowUpdates.sortedBy { it.currentBuildpack.name }
        detected.size shouldBe 2

        detected[0].currentBuildpack.name shouldBeEqualTo "paketo-buildpacks/java"
        detected[0].currentBuildpack.version shouldBeEqualTo SemanticVersion("21.4.0")
        detected[0].latestUpdate.version shouldBeEqualTo SemanticVersion("21.5.0")

        detected[1].currentBuildpack.name shouldBeEqualTo "paketo-buildpacks/nodejs"
        detected[1].currentBuildpack.version shouldBeEqualTo SemanticVersion("1.3.0")
        detected[1].latestUpdate.version shouldBeEqualTo SemanticVersion("1.4.0")
    }

    @Test
    fun `paketo buildpacks in github actions workflows are published when UPDATE_WORKFLOW_FILES is true`() {
        val fixtureDir = File("src/test/resources/github-actions-test")
        fixtureDir.exists() shouldBe true

        val settings = Settings(
            mapOf(
                GIT_HUB_API_URL.key to baseUrl,
                Setting.UPDATE_WORKFLOW_FILES.key to "true"
            )
        )
        val capturingPublisher = CapturingPublisher()
        val buildpackVersionChecker = BuildpackVersionChecker(
            fixtureDir,
            GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings),
            capturingPublisher,
            settings
        )

        val results = buildpackVersionChecker.performChecks()
        results shouldBeInstanceOf SuccessfulChecks::class

        val updates = capturingPublisher.updates().sortedBy { it.currentBuildpack.name }
        updates.size shouldBe 2

        updates[0].currentBuildpack.name shouldBeEqualTo "paketo-buildpacks/java"
        updates[0].latestUpdate.version shouldBeEqualTo SemanticVersion("21.5.0")

        updates[1].currentBuildpack.name shouldBeEqualTo "paketo-buildpacks/nodejs"
        updates[1].latestUpdate.version shouldBeEqualTo SemanticVersion("1.4.0")

        results.detectedWorkflowUpdates.size shouldBe 0
    }

    @Test
    fun `a failed publish should lead to failed check`() {
        val manifest = File("src/test/resources/manifest.yml")
        manifest.exists() shouldBe true

        val settings = Settings(mapOf(GIT_HUB_API_URL.key to baseUrl))
        val capturingPublisher = CapturingFailingPublisher()
        val buildpackVersionChecker = BuildpackVersionChecker(
            manifest,
            GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings),
            capturingPublisher
        )

        val results = buildpackVersionChecker.performChecks()
        results shouldBeInstanceOf FailedChecks::class
        when (results) {
            is FailedChecks -> {
                results.errors.size shouldBe 1
                results.errors.keys.first() shouldBeInstanceOf BuildpackUpdate::class
                results.errors.values.first() shouldBeInstanceOf RuntimeException::class
            }
            else -> {
                fail("Got wrong type of results", FailedChecks::class, results::class)
            }
        }
        capturingPublisher.updates().size shouldBe 0
    }


    class CapturingPublisher : Publisher {
        private val update = mutableListOf<BuildpackUpdate>()

        override fun publish(update: BuildpackUpdate) {
            this.update.add(update)
        }

        fun updates(): List<BuildpackUpdate> = update
    }

    class CapturingFailingPublisher : Publisher {

        override fun publish(update: BuildpackUpdate) {
            throw RuntimeException("We cannot publish, e.g. GitHub is down…")
        }

        fun updates(): List<BuildpackUpdate> = emptyList()
    }

    companion object {
        private val server: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
            .also { server ->
                context(
                    server,
                    "/repos/cloudfoundry/staticfile-buildpack/releases/latest",
                    "/github-latest-staticfile.json"
                )
            }.also { server ->
                context(server, "/repos/cloudfoundry/java-buildpack/releases/latest", "/github-latest-java.json")
            }.also { server ->
                context(
                    server,
                    "/repos/paketo-buildpacks/java/releases/latest",
                    "/github-latest-paketo-java.json"
                )
            }.also { server ->
                context(
                    server,
                    "/repos/paketo-buildpacks/nodejs/releases/latest",
                    "/github-latest-paketo-nodejs.json"
                )
            }

        private fun context(server: HttpServer, path: String, responseFile: String) {
            server.createContext(path) { exchange ->
                exchange.sendResponse(
                    status = 200,
                    headers = mapOf("content-type" to "application/json;charset=utf-8"),
                    body = readTestResource(responseFile)
                )
            }
        }

        private fun readTestResource(resourcePath: String) =
            this::class.java.getResourceAsStream(resourcePath)?.bufferedReader()?.readText()
                ?: error("Failed to read $resourcePath")

        private val baseUrl = "http://localhost:${server.address.port}"

        private fun HttpExchange.sendResponse(
            status: Int = 200,
            body: String = "",
            headers: Map<String, String> = emptyMap(),
        ) {
            responseHeaders.putAll(headers.mapValues { listOf(it.value) })
            sendResponseHeaders(status, body.length.toLong())
            val os = responseBody
            os.write(body.encodeToByteArray())
            os.close()
        }

        @BeforeAll
        @JvmStatic
        fun startServer() {
            server.executor = null
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stopServer() {
            server.stop(0)
        }
    }
}
