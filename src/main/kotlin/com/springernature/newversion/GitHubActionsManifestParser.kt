package com.springernature.newversion

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

object GitHubActionsManifestParser {

    private val LOG: Logger = LoggerFactory.getLogger(GitHubActionsManifestParser::class.java)

    fun load(dir: File): Sequence<PaketoManifestLoadResult> = dir.walk()
        .filter { it.isFile }
        .filterNot { it.invariantSeparatorsPath.contains("/.git/") }
        .filter { isWorkflowFile(it) }
        .onEach { LOG.debug("Found GitHub Actions workflow {}", it) }
        .map { file ->
            try {
                val parsed = readWorkflowFile(file)
                val buildpacks = parsed.jobs.values
                    .flatMap { it.steps }
                    .mapNotNull { it.with?.buildpacks }
                    .filter { it.contains(":") }
                    .map { VersionedBuildpack.createPaketo(it) }
                PaketoManifest(buildpacks, file)
            } catch (e: Exception) {
                LOG.warn("Failed to parse workflow file {}", file, e)
                FailedPaketoManifest(file, e)
            }
        }

    private fun isWorkflowFile(file: File): Boolean {
        val path = file.invariantSeparatorsPath
        val ext = file.extension.lowercase()
        return (ext == "yml" || ext == "yaml") && path.contains("/.github/workflows/")
    }

    private fun readWorkflowFile(f: File): WorkflowFile = ObjectMapper(YAMLFactory())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerKotlinModule()
        .readValue(f)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class WorkflowFile(
        val jobs: Map<String, WorkflowJob> = emptyMap()
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class WorkflowJob(
        val steps: List<WorkflowStep> = emptyList()
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class WorkflowStep(
        val with: WithBlock? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class WithBlock(
        val buildpacks: String? = null
    )
}
