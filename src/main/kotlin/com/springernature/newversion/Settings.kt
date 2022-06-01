package com.springernature.newversion

enum class Setting(val key: String, val defaultValue: String) {
    GIT_HUB_API_URL("GITHUB_API_URL", "https://api.github.com"),
    AUTHOR_EMAIL("AUTHOR_EMAIL", "do_not_reply@springernature.com"),
    AUTHOR_NAME("AUTHOR_NAME", "Buildpack Update Action"),
    GITHUB_STEP_SUMMARY("GITHUB_STEP_SUMMARY", "/dev/null"),
    GITHUB_STEP_SUMMARY_ENABLED("GITHUB_STEP_SUMMARY_ENABLED", false.toString())
}

class Settings(private val values: Map<String, String> = mapOf()) {

    fun lookup(setting: Setting) =
        values[setting.key] ?: setting.defaultValue

}