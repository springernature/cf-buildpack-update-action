package com.springernature.newversion

import org.amshove.kluent.shouldBe
import org.junit.jupiter.api.Test
import java.net.Authenticator
import java.net.CookieHandler
import java.net.ProxySelector
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters

class BuildpackUpdateTest {

    @Test
    fun `a failed manifest load returns an empty list`() {
        val buildpack = BuildpackUpdate.create(FailedManifest("/a/path", RuntimeException("test")), DummyHttpClient(), Settings())

        buildpack.isEmpty() shouldBe true
    }

    // TODO successful load

    @Test
    fun `a newer version is considered an update`() {
        val possibleUpdate = BuildpackUpdate(
            "a/path",
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.2.0")),
            SemanticVersion("1.2.3")
        )

        possibleUpdate.hasUpdate() shouldBe true
    }

    @Test
    fun `an older version is not considered an update`() {
        val possibleUpdate = BuildpackUpdate(
            "a/path",
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.5.0")),
            SemanticVersion("1.2.4")
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    @Test
    fun `the same version is not considered an update`() {
        val possibleUpdate = BuildpackUpdate(
            "a/path",
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("2.3.4")),
            SemanticVersion("2.3.4")
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    @Test
    fun `a buildpack from HEAD has no updates`() {
        val possibleUpdate = BuildpackUpdate(
            "a/path",
            VersionedBuildpack("test/buildpack1", "https://a.host/path", Latest()),
            SemanticVersion("2.3.4")
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    @Test
    fun `semantic versioning is used for upgrade checks`() {
        val possibleUpdate = BuildpackUpdate(
            "a/path",
            VersionedBuildpack("test/buildpack1", "https://a.host/path", SemanticVersion("1.3")),
            SemanticVersion("1.2.4")
        )

        possibleUpdate.hasUpdate() shouldBe false
    }

    private class DummyHttpClient : HttpClient() {
        override fun cookieHandler(): Optional<CookieHandler> {
            TODO("Not yet implemented")
        }

        override fun connectTimeout(): Optional<Duration> {
            TODO("Not yet implemented")
        }

        override fun followRedirects(): Redirect {
            TODO("Not yet implemented")
        }

        override fun proxy(): Optional<ProxySelector> {
            TODO("Not yet implemented")
        }

        override fun sslContext(): SSLContext {
            TODO("Not yet implemented")
        }

        override fun sslParameters(): SSLParameters {
            TODO("Not yet implemented")
        }

        override fun authenticator(): Optional<Authenticator> {
            TODO("Not yet implemented")
        }

        override fun version(): Version {
            TODO("Not yet implemented")
        }

        override fun executor(): Optional<Executor> {
            TODO("Not yet implemented")
        }

        override fun <T : Any?> send(
            request: HttpRequest?,
            responseBodyHandler: HttpResponse.BodyHandler<T>?
        ): HttpResponse<T> {
            TODO("Not yet implemented")
        }

        override fun <T : Any?> sendAsync(
            request: HttpRequest?,
            responseBodyHandler: HttpResponse.BodyHandler<T>?
        ): CompletableFuture<HttpResponse<T>> {
            TODO("Not yet implemented")
        }

        override fun <T : Any?> sendAsync(
            request: HttpRequest?,
            responseBodyHandler: HttpResponse.BodyHandler<T>?,
            pushPromiseHandler: HttpResponse.PushPromiseHandler<T>?
        ): CompletableFuture<HttpResponse<T>> {
            TODO("Not yet implemented")
        }
    }

}