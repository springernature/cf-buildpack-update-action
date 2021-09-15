package com.springernature.newversion

enum class Setting(val key: String, val defaultValue: String) {
    GIT_HUB_API_URL("GITHUB_API_URL", "https://api.github.com")
}

class Settings(private val values: Map<String, String> = mapOf()) {

    fun lookup(setting: Setting) =
        values[setting.key] ?: setting.defaultValue

}