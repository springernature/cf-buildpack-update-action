package com.springernature.newversion

import org.amshove.kluent.shouldBe
import org.junit.jupiter.api.Test
import java.io.File

class YAMLParserTest {

    private fun resourcePath(path: String): String {
        return File("src/test/resources/").absolutePath + "/" + path
    }

    @Test
    fun testLoadingDoesntFailOnDirectories() {
        val loadManifests = loadManifests(resourcePath("/directory-name-test"))
        val failedCount = loadManifests.filterIsInstance<FailedManifest>().count()
        failedCount shouldBe 0
    }

}

