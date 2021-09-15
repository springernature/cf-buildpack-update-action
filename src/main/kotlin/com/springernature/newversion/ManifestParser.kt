package com.springernature.newversion

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

sealed class ManifestLoadResult
data class Manifest(val applications: List<CFApplication>, val path: String = "") : ManifestLoadResult()
data class FailedManifest(
    val path: String,
    val error: Exception
) : ManifestLoadResult()

private fun readManifest(f: File): Manifest = ObjectMapper(YAMLFactory())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .registerKotlinModule().let {
        it.readValue(f)
    }

private fun loadManifests(dir: File): Sequence<ManifestLoadResult> = dir.walk()
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

fun loadManifests(path: String): Sequence<ManifestLoadResult> = loadManifests(File(path))
