package com.springernature.newversion

import com.springernature.newversion.Setting.GIT_HUB_API_URL
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.http.HttpClient

class GitHubBuildpackUpdateCheckerTest {

    @Test
    fun `we can query GitHub to find if updates are available`() {

        val settings = Settings(mapOf(GIT_HUB_API_URL.key to baseUrl))
        val buildpackUpdateChecker = GitHubBuildpackUpdateChecker(HttpClient.newBuilder().build(), settings)
        val buildpack = VersionedBuildpack(
            "cloudfoundry/java-buildpack",
            "https://github.com/cloudfoundry/java-buildpack",
            SemanticVersion("4.0.20")
        )
        val latestBuildpackVersion = buildpackUpdateChecker.findLatestVersion(buildpack)

        latestBuildpackVersion.toSemVer().let {
            it.major.shouldBeEqualTo(4)
            it.minor.shouldBeEqualTo(41)
        }
    }

    companion object {
        private val server: HttpServer = HttpServer.create(InetSocketAddress(0), 0).also { server ->
            server.createContext("/repos/cloudfoundry/java-buildpack/releases/latest") { exchange ->
                exchange.sendResponse(
                    status = 200,
                    headers = mapOf("content-type" to "application/json;charset=utf-8"),
                    body = readTestResource("/github-latest-java.json")
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
