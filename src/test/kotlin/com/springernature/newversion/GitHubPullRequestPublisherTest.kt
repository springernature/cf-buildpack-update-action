package com.springernature.newversion

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.File

class GitHubPullRequestPublisherTest {

    @Test
    fun `we don't create a new pull request when a pull-request with the named branch already exists`() {
        val shell = CapturingShell(mapOf(
            ("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" },
            ("hub" to listOf("pr", "list", "-s", "open", "-f", "'%H%n'") to {
                """
                        update/scalatest-3.2.9
                        update/handlebars-4.1.2
                        buildpack-update/update-test-buildpack-2.3.6
                        update/log4j-core-2.13.3
                    """.trimIndent()
            })))
        val publisher = GitHubPullRequestPublisher(shell, Settings())

        publisher.publish(
            BuildpackUpdate(
                createTestManifest(),
                VersionedBuildpack("test/buildpack", "https://a.host/test/buildpack", SemanticVersion("2.0.4")),
                SemanticVersion("2.3.6")
            )
        )

        shell.commands shouldBeEqualTo listOf(
            "git" to listOf("remote", "prune", "origin"),
            "git" to listOf("fetch", "--prune", "--prune-tags"),
            "git" to listOf("config", "user.name", "Buildpack Update Action"),
            "git" to listOf("config", "user.email", "do_not_reply@springernature.com"),
            "git" to listOf("rev-parse", "--abbrev-ref", "HEAD"),
            "hub" to listOf("pr", "list", "-s", "open", "-f", "'%H%n'"),
            "git" to listOf("switch", "base-branch")
        )
    }

    @Test
    fun `create a pull request when no existing pull request with the named branch is present`() {
        val shell = CapturingShell(
            mapOf(
                ("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" },
                ("hub" to listOf("pr", "list", "-s", "open", "-f", "'%H%n'")) to {
                    """
                        update/scalatest-3.2.9
                        update/handlebars-4.1.2
                        update/log4j-core-2.13.3
                    """.trimIndent()
                }
            )
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
            "git" to listOf("remote", "prune", "origin"),
            "git" to listOf("fetch", "--prune", "--prune-tags"),
            "git" to listOf("config", "user.name", "Buildpack Update Action"),
            "git" to listOf("config", "user.email", "do_not_reply@springernature.com"),
            "git" to listOf("rev-parse", "--abbrev-ref", "HEAD"),
            "hub" to listOf("pr", "list", "-s", "open", "-f", "'%H%n'"),
            "git" to listOf("checkout", "-B", "buildpack-update/update-test-buildpack-2.3.6", "--quiet"),
            "git" to listOf(
                "commit", "-a", "--quiet",
                "-m", "Update test/buildpack to 2.3.6",
                "--author='Buildpack Update Action <do_not_reply@springernature.com>'"
            ),
            "hub" to listOf(
                "pull-request", "--push",
                "--message='Update test/buildpack to 2.3.6 in $manifest\n\nUpdate test/buildpack from 2.0.4 to 2.3.6'",
                "--base=buildpack-update/update-test-buildpack-2.3.6", "--labels=buildpack-update"
            ),
            "git" to listOf("switch", "base-branch")
        )
    }

    @Test
    fun `clean up old pull requests when one for a newer version is created`() {
        val shell = CapturingShell(
            mapOf(
                ("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" },
                ("hub" to listOf("pr", "list", "-s", "open", "-f", "'%H%n'")) to {
                    """
                        update/scalatest-3.2.9
                        buildpack-update/update-test-buildpack-2.3.5
                        update/handlebars-4.1.2
                        buildpack-update/update-test-buildpack-2.2.1
                        buildpack-update/update-test-buildpack-2.4.0
                        update/log4j-core-2.13.3
                    """.trimIndent()
                }
            )
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
            "git" to listOf("remote", "prune", "origin"),
            "git" to listOf("fetch", "--prune", "--prune-tags"),
            "git" to listOf("config", "user.name", "Buildpack Update Action"),
            "git" to listOf("config", "user.email", "do_not_reply@springernature.com"),
            "git" to listOf("rev-parse", "--abbrev-ref", "HEAD"),
            "hub" to listOf("pr", "list", "-s", "open", "-f", "'%H%n'"),
            "git" to listOf("checkout", "-B", "buildpack-update/update-test-buildpack-2.3.6", "--quiet"),
            "git" to listOf(
                "commit", "-a", "--quiet",
                "-m", "Update test/buildpack to 2.3.6",
                "--author='Buildpack Update Action <do_not_reply@springernature.com>'"
            ),
            "hub" to listOf(
                "pull-request", "--push",
                "--message='Update test/buildpack to 2.3.6 in $manifest\n\nUpdate test/buildpack from 2.0.4 to 2.3.6'",
                "--base=buildpack-update/update-test-buildpack-2.3.6", "--labels=buildpack-update"
            ),
            "git" to listOf("push", "origin", ":buildpack-update/update-test-buildpack-2.3.5"),
            "git" to listOf("push", "origin", ":buildpack-update/update-test-buildpack-2.2.1"),
            "git" to listOf("switch", "base-branch")
        )
    }

    @Test
    fun `the manifest should be updated to point at the new version of the buildpack`() {
        val shell = CapturingShell(
            mapOf(
                ("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" }
            )
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

        manifest.readText() shouldBeEqualTo """
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

    private fun createTestManifest(): File =
        File.createTempFile("github-pull-request-publisher-test-manifest", ".yml").also {
            it.deleteOnExit()
            it.writeText(
                """
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
