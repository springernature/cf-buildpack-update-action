package com.springernature.manifest

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File


sealed class ManifestLoadResult
data class Manifest(val applications: List<CFApplication>) : ManifestLoadResult()
data class FailedManifest(
    val path: String,
    val error: Exception
) : ManifestLoadResult()


data class CFApplication(val buildpacks: List<VersionedBuildpack>)
data class VersionedBuildpack(val url: String, val version: String) {
    companion object {
        @JvmStatic
        @JsonCreator
        fun create(value: String) = VersionedBuildpack(value.buildpackUrl(), value.buildpackVersion())

        private fun String.buildpackUrl(): String = "(.*github.com/.*)#v.*".toRegex().find(this)?.groups?.get(1)?.value
            ?: throw Exception("buildpack url is in wrong format: $this")

        private fun String.buildpackVersion(): String =
            ".*github.com/.*#v(.*)".toRegex().find(this)?.groups?.get(1)?.value
                ?: throw Exception("buildpack url is in wrong format: $this")
    }
}

fun readManifest(f: File): Manifest {
    val mapper = ObjectMapper(YAMLFactory())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerKotlinModule()

    return mapper.readValue(f)
}

fun loadManifests(dir: File): Sequence<ManifestLoadResult> {
    return dir.walk()
       .filterNot { it.isDirectory }
        .filterNot { it.path.contains("\\.git/") }
        .filter { it.name.contains("manifest") }
        .onEach { println(it) }
        .map {
            try {
                readManifest(it)
            } catch (e: Exception) {
                FailedManifest(it.path, e)
            }
        }
}

fun loadManifests(path: String): Sequence<ManifestLoadResult> {
    return loadManifests(File(path))
}

fun main() {
    loadManifests(".").forEach { when(it) {
        is Manifest -> println("yay: $it")
        is FailedManifest -> println("nay: $it")
    } }
}
