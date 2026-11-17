package com.springernature.newversion

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
                val buildpacks =
                    parsed
                        .buildpacks()
                        .filter { it.contains(":") }
                        .map { VersionedBuildpack.createPaketo(it) }
                PaketoManifest(buildpacks, file)
            } catch (e: Exception) {
                LOG.warn("Failed to parse halfpipe manifest {}", file, e)
                FailedPaketoManifest(file, e)
            }
        }

    private fun readHalfpipeFile(f: File): HalfpipeTasks = ObjectMapper(YAMLFactory())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerKotlinModule()
        .readValue(f)

    /**
     * A halfpipe task can be a buildpack task, a nested parallel or sequence task list, or any other ignored task.
     * The buildpack task has a `buildpacks` property, while the nested pipeline has a `tasks` property.
     * Only tasks of type `buildpack` contribute their `buildpacks`; any other task is searched for nested `tasks`.
     */
    private data class HalfpipeTasks(
        val type: String? = null,
        val tasks: List<HalfpipeTasks>? = null,
        val buildpacks: List<String> = emptyList(),
    )

    private fun HalfpipeTasks.buildpacks(): List<String> = when (type) {
        "buildpack" -> buildpacks
        else -> tasks.orEmpty().flatMap { it.buildpacks() }
    }
}
