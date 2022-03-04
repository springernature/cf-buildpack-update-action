package com.springernature.newversion

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

sealed class ManifestLoadResult
data class Manifest(val applications: List<CFApplication>, val path: File = File(".")) : ManifestLoadResult()
data class FailedManifest(
    val path: File,
    val error: Exception
) : ManifestLoadResult()

object ManifestParser {

    private val LOG: Logger = LoggerFactory.getLogger(ManifestParser::class.java)

    fun load(dir: File): Sequence<ManifestLoadResult> = dir.walk()
        .filter { it.isFile }
        .filterNot { it.path.contains("\\.git/") }
        .filter { it.name.endsWith(".yml") || it.name.endsWith(".yaml") }
        .filter { it.name.contains("manifest") || it.name.contains("cf") }
        .onEach { LOG.debug("Found manifest {}", it) }
        .map {
            try {
                readManifest(it).copy(path = it)
            } catch (e: Exception) {
                FailedManifest(it, e)
            }
        }

    private fun readManifest(f: File): Manifest = ObjectMapper(YAMLFactory())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerKotlinModule().readValue(f)

}
