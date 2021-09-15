package com.springernature.newversion

import com.springernature.newversion.Setting.GIT_HUB_API_URL
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SettingsTest {

    @Test
    fun `a setting can be found in the provided values`() {
        Settings(mapOf(GIT_HUB_API_URL.key to "http://a.github/"))
            .lookup(GIT_HUB_API_URL) shouldBeEqualTo "http://a.github/"
    }

    @Test
    fun `a setting will have the default value used if no override exists`() {
        Settings(mapOf())
            .lookup(GIT_HUB_API_URL) shouldBeEqualTo "https://api.github.com"
    }

}