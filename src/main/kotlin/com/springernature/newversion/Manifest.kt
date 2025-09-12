package com.springernature.newversion

import com.fasterxml.jackson.annotation.JsonCreator
import io.github.z4kn4fein.semver.Version as SemVer

data class CFApplication(
    private val buildpacks: List<VersionedBuildpack> = listOf(),
    private val buildpack: VersionedBuildpack? = null
) {
    fun buildpacks() = buildpack?.let { buildpacks.plus(buildpack) } ?: buildpacks
}

data class VersionedBuildpack(val name: String, val url: String, val version: Version, val tag: GitTag?) {

    companion object {

        private const val SEMVER_REGEX = "\\d+(?:\\.\\d+(?:\\.\\d+)?)?"

        @JvmStatic
        @JsonCreator
        fun create(value: String) =
            VersionedBuildpack(value.name(), value.buildpackUrl(), value.buildpackVersion(), value.buildpackTag())

        private fun String.buildpackUrl(): String =
            "(.*github.com/.*?)(?:\\.git)?(?:#v?$SEMVER_REGEX)?$".toRegex().find(this)?.groups?.get(1)?.value
                ?: "https://github.com/cloudfoundry/${translateBuiltIn(this)}"

        private fun String.buildpackVersion(): Version =
            ".*github.com/.*#v?($SEMVER_REGEX)$".toRegex()
                .find(this)?.groups?.get(1)?.value?.let { SemanticVersion(it) }
                ?: Unparseable

        private fun String.buildpackTag(): GitTag? =
            ".*github.com/.*#(v?$SEMVER_REGEX)$".toRegex().find(this)?.groups?.get(1)?.value?.let { GitTag(it) }

        private fun String.name(): String =
            ".*github.com/(.*?)(?:\\.git)?(?:#v$SEMVER_REGEX)?$".toRegex().find(this)?.groups?.get(1)?.value
                ?: "cloudfoundry/${translateBuiltIn(this)}"

        private fun translateBuiltIn(builtInName: String) = builtInName.replace("_", "-")
    }
}

sealed class Version
data class SemanticVersion(private val versionString: String) : Version() {
    fun toSemVer(): SemVer = SemVer.parse(versionString.trimStart('v'), strict = false)
    override fun toString() = versionString
}

object Unparseable : Version()
