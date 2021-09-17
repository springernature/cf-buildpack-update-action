package com.springernature.newversion

import com.fasterxml.jackson.annotation.JsonCreator
import net.swiftzer.semver.SemVer

data class CFApplication(val buildpacks: List<VersionedBuildpack>)

data class VersionedBuildpack(val name: String, val url: String, val version: Version) {

    companion object {

        @JvmStatic
        @JsonCreator
        fun create(value: String) =
            VersionedBuildpack(value.name(), value.buildpackUrl(), value.buildpackVersion())

        private fun String.buildpackUrl(): String =
            "(.*github.com/.*?)(?:#v.*)?$".toRegex().find(this)?.groups?.get(1)?.value
                ?: throw Exception("Cannot parse buildpack URL: $this")

        private fun String.buildpackVersion(): Version =
            ".*github.com/.*#v(.*)$".toRegex().find(this)?.groups?.get(1)?.value?.let { SemanticVersion(it) }
                ?: Latest()

        private fun String.name(): String =
            ".*github.com/(.*?)(?:#v.*)?$".toRegex().find(this)?.groups?.get(1)?.value
                ?: throw Exception("Cannot parse buildpack URL: $this")
    }
}

sealed class Version
data class SemanticVersion(private val versionString: String) : Version() {
    fun toSemVer(): SemVer = SemVer.parse(versionString.trimStart('v'))
}

class Latest : Version() {
    override fun equals(other: Any?): Boolean = when (other) {
        is Latest -> true
        else -> false
    }

    override fun hashCode(): Int = 1
}
