package com.springernature.newversion

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.swiftzer.semver.SemVer
import java.io.File

sealed class ManifestLoadResult
data class Manifest(val applications: List<CFApplication>, val path: String = "") : ManifestLoadResult()
data class FailedManifest(
    val path: String,
    val error: Exception
) : ManifestLoadResult()

sealed class Version
data class SemanticVersion(private val versionString: String) : Version() {
    fun toSemVer(): SemVer {
        return SemVer.parse(versionString.trimStart('v'))
    }
}
class Latest : Version() {
    override fun equals(other: Any?): Boolean = when (other) {
        is Latest -> true
        else -> false
    }

    override fun hashCode(): Int = 1
}

data class CFApplication(val buildpacks: List<VersionedBuildpack>)
data class VersionedBuildpack(val name: String, val url: String, val version: Version) {
    companion object {
        @JvmStatic
        @JsonCreator
        fun create(value: String) =
            VersionedBuildpack(value.name(), value.buildpackUrl(), value.buildpackVersion())

        private fun String.buildpackUrl(): String = "(.*github.com/.*?)(?:#v.*)?$".toRegex().find(this)?.groups?.get(1)?.value
            ?: throw Exception("Cannot parse buildpack URL: $this")

        private fun String.buildpackVersion(): Version =
            ".*github.com/.*#v(.*)$".toRegex().find(this)?.groups?.get(1)?.value?.let { SemanticVersion(it) }
                ?: Latest()

        private fun String.name(): String =
            ".*github.com/(.*?)(?:#v.*)?$".toRegex().find(this)?.groups?.get(1)?.value
                ?: throw Exception("Cannot parse buildpack URL: $this")

    }
}

private fun readManifest(f: File): Manifest {
    val mapper = ObjectMapper(YAMLFactory())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerKotlinModule()

    return mapper.readValue(f)
}

fun loadManifests(dir: File): Sequence<ManifestLoadResult> {
    return dir.walk()
        .filter { it.isFile }
        .filterNot { it.path.contains("\\.git/") }
        .filter { it.name.endsWith(".yml") || it.name.endsWith(".yaml") }
        .filter { it.name.contains("manifest") }
        .onEach { println(it) }
        .map {
            try {
                readManifest(it).copy(path = it.path)
            } catch (e: Exception) {
                FailedManifest(it.path, e)
            }
        }
}

fun loadManifests(path: String): Sequence<ManifestLoadResult> {
    return loadManifests(File(path))
}
