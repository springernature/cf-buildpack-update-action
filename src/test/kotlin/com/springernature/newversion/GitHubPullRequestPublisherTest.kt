package com.springernature.newversion

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.File

class GitHubPullRequestPublisherTest {

    @Test
    fun `we don't create a new pull request when the branch already exists`() {
        val shell = CapturingShell(mapOf(("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" }))
        val publisher = GitHubPullRequestPublisher(shell, Settings())

        publisher.publish(
            BuildpackUpdate(
                createTestManifest(),
                VersionedBuildpack("test/buildpack", "https://a.host/test/buildpack", SemanticVersion("2.0.4")),
                SemanticVersion("2.3.6")
            )
        )

        shell.commands shouldBeEqualTo listOf(
            "git" to listOf("rev-parse", "--abbrev-ref", "HEAD"),
            "git" to listOf("remote", "prune", "origin"),
            "git" to listOf("fetch", "--prune", "--prune-tags"),
            "git" to listOf("switch", "update-test-buildpack"), // this will succeed, meaning the branch already exists
            "git" to listOf("switch", "base-branch")
        )
    }

    @Test
    fun `create a pull request when no existing branch is present`() {
        val shell = CapturingShell(
            mapOf(
                ("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" },
                ("git" to listOf("switch", "update-test-buildpack")) to { throw RuntimeException("Already exists") })
        )
        val publisher = GitHubPullRequestPublisher(shell, Settings())
        val manifest = createTestManifest()

        publisher.publish(
            BuildpackUpdate(
                manifest,
                VersionedBuildpack("test/buildpack", "https://a.host/test/buildpack", SemanticVersion("2.0.4")),
                SemanticVersion("2.3.6")
            )
        )

        shell.commands shouldBeEqualTo listOf(
            "git" to listOf("rev-parse", "--abbrev-ref", "HEAD"),
            "git" to listOf("remote", "prune", "origin"),
            "git" to listOf("fetch", "--prune", "--prune-tags"),
            "git" to listOf("switch", "update-test-buildpack"),
            "git" to listOf("checkout", "-B", "update-test-buildpack", "--quiet"),
            "git" to listOf(
                "commit", "-a", "--quiet",
                "--message", "update test/buildpack to 2.3.6",
                "--author", "buildpack update action <do_not_reply@springernature.com>"
            ),
            "hub" to listOf(
                "pull-request", "--push",
                "--message='update test/buildpack to 2.3.6 in $manifest\n\nupdate test/buildpack from 2.0.4 to 2.3.6'",
                "--base=update-test-buildpack", "--labels=buildpack-update"
            ),
            "git" to listOf("switch", "base-branch")
        )
    }

    @Test
    fun `the manifest should be updated to point at the new version of the buildpack`() {
        val shell = CapturingShell(
            mapOf(
                ("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" },
                ("git" to listOf("switch", "update-test-buildpack")) to { throw RuntimeException("Already exists") })
        )
        val publisher = GitHubPullRequestPublisher(shell, Settings())
        val manifest = createTestManifest()

        publisher.publish(
            BuildpackUpdate(
                manifest,
                VersionedBuildpack("test/buildpack", "https://a.host/test/buildpack", SemanticVersion("2.0.4")),
                SemanticVersion("2.3.6")
            )
        )

        File(manifest).bufferedReader().readText() shouldBeEqualTo """
            ---
            applications:
            - name: dummy-manifest-for-testing
              instances: 1
              health-check-type: process
              no-route: true
              buildpacks:
              - https://a.host/test/buildpack#v2.3.6
        """.trimIndent()
    }

    private fun createTestManifest(): String {
        return File.createTempFile("github-pull-request-publisher-test-manifest", ".yml").also {
            it.deleteOnExit()
            it.bufferedWriter().use { writer ->
                writer.write("""
                    ---
                    applications:
                    - name: dummy-manifest-for-testing
                      instances: 1
                      health-check-type: process
                      no-route: true
                      buildpacks:
                      - https://a.host/test/buildpack#v2.0.4
                """.trimIndent()
                )
            }
        }.absolutePath
    }

    private class CapturingShell(private val commandOutput: Map<Pair<String, List<String>>, () -> String> = mapOf()) :
        Shell {
        val commands = mutableListOf<Pair<String, List<String>>>()
        override fun run(workingDirectory: File?, script: Script.() -> String): String {
            val capturingScript = CapturingScript(commandOutput)
            return try {
                capturingScript.script()
            } finally {
                commands.addAll(capturingScript.commands)
            }
        }
    }

    private class CapturingScript(private val commandOutput: Map<Pair<String, List<String>>, () -> String> = mapOf()) :
        Script {
        val commands = mutableListOf<Pair<String, List<String>>>()
        override fun command(command: String, arguments: List<String>): String {
            commands.add(command to arguments)
            return commandOutput[command to arguments]?.invoke() ?: ""
        }
    }

}
