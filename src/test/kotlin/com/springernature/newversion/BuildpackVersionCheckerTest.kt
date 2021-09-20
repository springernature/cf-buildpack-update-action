package com.springernature.newversion

import com.springernature.newversion.Setting.GIT_HUB_API_URL
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.net.InetSocketAddress
import java.net.http.HttpClient
import kotlin.test.assertNotNull

class BuildpackVersionCheckerTest {

    @Test
    fun `we can query GitHub to find if updates are available`() {
        val manifest = File("src/test/resources/manifest.yml")
        manifest.exists() shouldBe true

        val settings = Settings(mapOf(GIT_HUB_API_URL.key to baseUrl))
        val capturingPublisher = CapturingPublisher()
        val buildpackVersionChecker = BuildpackVersionChecker(
            manifest,
            GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings),
            capturingPublisher
        )

        buildpackVersionChecker.performChecks()

        val lastUpdate = capturingPublisher.lastUpdate
        assertNotNull(lastUpdate)

        lastUpdate.currentBuildpack.name shouldBeEqualTo "cloudfoundry/staticfile-buildpack"
        lastUpdate.currentBuildpack.version shouldBeEqualTo SemanticVersion("1.5.17")
        lastUpdate.latestVersion shouldBeEqualTo SemanticVersion("1.5.24")
    }

    @Test
    fun `find latest version if version only specifies major and minor but no patch version`() {
        val manifest = File("src/test/resources/manifest-no-patch-version-specified.yml")
        manifest.exists() shouldBe true

        val settings = Settings(mapOf(GIT_HUB_API_URL.key to baseUrl))
        val capturingPublisher = CapturingPublisher()
        val buildpackVersionChecker = BuildpackVersionChecker(
            manifest,
            GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings),
            capturingPublisher
        )

        buildpackVersionChecker.performChecks()

        val lastUpdate = capturingPublisher.lastUpdate
        assertNotNull(lastUpdate)

        lastUpdate.currentBuildpack.name shouldBeEqualTo "cloudfoundry/java-buildpack"
        lastUpdate.currentBuildpack.version shouldBeEqualTo SemanticVersion("4.20")
        lastUpdate.latestVersion shouldBeEqualTo SemanticVersion("4.41")
    }

    class CapturingPublisher : Publisher {
        var lastUpdate: BuildpackUpdate? = null

        override fun publish(update: BuildpackUpdate) {
            lastUpdate = update
        }
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
            headers: Map<String, String> = emptyMap()
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
