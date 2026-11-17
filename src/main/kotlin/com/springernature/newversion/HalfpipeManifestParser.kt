package com.springernature.newversion

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

sealed class PaketoManifestLoadResult
data class PaketoManifest(val buildpacks: List<VersionedBuildpack>, val path: File) : PaketoManifestLoadResult()
data class FailedPaketoManifest(val path: File, val error: Exception) : PaketoManifestLoadResult()

object HalfpipeManifestParser {

    private val LOG: Logger = LoggerFactory.getLogger(HalfpipeManifestParser::class.java)

    private val HALFPIPE_FILENAMES = setOf(".halfpipe.io", ".halfpipe.io.yaml", ".halfpipe.io.yml")

    fun load(dir: File): Sequence<PaketoManifestLoadResult> = dir.walk()
        .filter { it.isFile }
        .filterNot { it.invariantSeparatorsPath.contains("/.git/") }
        .filter { it.name in HALFPIPE_FILENAMES }
        .onEach { LOG.debug("Found halfpipe manifest {}", it) }
        .map { file ->
            try {
                val parsed = readHalfpipeFile(file)
                val buildpacks = parsed.tasks
                    .flatMap {it.buildpacks() }
                    .filter { it.contains(":") }
                    .map { VersionedBuildpack.createPaketo(it) }
                PaketoManifest(buildpacks, file)
            } catch (e: Exception) {
                LOG.warn("Failed to parse halfpipe manifest {}", file, e)
                FailedPaketoManifest(file, e)
            }
        }

    private fun readHalfpipeFile(f: File): HalfpipeFile = ObjectMapper(YAMLFactory())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerKotlinModule()
        .readValue(f)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class HalfpipeFile(val tasks: List<HalfpipeTask> = emptyList())

    private fun HalfpipeTask.buildpacks(): List<String> = when (this) {
        is HalfpipeBuildpackTask -> this.buildpacks
        is HalfpipeTasks -> this.tasks.flatMap { it.buildpacks() }
        is HalfpipeIgnoredTask -> emptyList()
    }

    private sealed interface HalfpipeTask {
        val type: String

        companion object {
            @JsonCreator
            @JvmStatic
            fun creator(
                type: String = "",
                tasks: List<HalfpipeTask> = listOf(),
                buildpacks: List<String> = emptyList()
            ): HalfpipeTask {
                val result = when (type) {
                    "parallel", "sequence" -> HalfpipeTasks(type, tasks)
                    "buildpack" -> HalfpipeBuildpackTask(type, buildpacks)
                    else -> HalfpipeIgnoredTask(type)
                }
                return result
            }
        }
    }

    private data class HalfpipeBuildpackTask(
        override val type: String,
        val buildpacks: List<String>
    ):HalfpipeTask

    private data class HalfpipeTasks(
        override val type: String,
        val tasks: List<HalfpipeTask> = listOf(),
    ) : HalfpipeTask

    private data class HalfpipeIgnoredTask(
        override val type: String,
    ) : HalfpipeTask
}
