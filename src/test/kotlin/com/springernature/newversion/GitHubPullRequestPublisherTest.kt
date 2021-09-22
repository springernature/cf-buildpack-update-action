package com.springernature.newversion

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.junit.jupiter.api.Test
import java.io.File

class GitHubPullRequestPublisherTest {

    @Test
    fun `we don't create a new pull request when a pull-request with the named branch already exists`() {
        val shell = CapturingShell(
            mapOf(
                ("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" },
                ("git" to listOf("branch", "-r") to {
                    """
                        origin/HEAD -> origin/main
                        origin/main
                        origin/update/scalatest-3.2.9
                        origin/update/handlebars-4.1.2
                        origin/buildpack-update/test-buildpack-2.3.6
                        origin/update/log4j-core-2.13.3
                    """.trimIndent()
                })
            )
        )
        val publisher = GitHubPullRequestPublisher(shell, Settings())

        publisher.publish(
            BuildpackUpdate(
                listOf(createTestManifest()),
                VersionedBuildpack(
                    "test/buildpack",
                    "https://a.host/test/buildpack",
                    SemanticVersion("2.0.4"),
                    GitTag("v2.0.4")
                ),
                BuildpackVersion(SemanticVersion("2.3.6"), GitTag("v2.3.6"))
            )
        )

        shell.commands shouldBeEqualTo listOf(
            "git" to listOf("remote", "prune", "origin"),
            "git" to listOf("fetch", "--prune", "--prune-tags"),
            "git" to listOf("config", "user.name", "Buildpack Update Action"),
            "git" to listOf("config", "user.email", "do_not_reply@springernature.com"),
            "git" to listOf("rev-parse", "--abbrev-ref", "HEAD"),
            "git" to listOf("branch", "-r"),
            "git" to listOf("switch", "base-branch")
        )
    }

    @Test
    fun `create a pull request when no existing pull request with the named branch is present`() {
        val shell = CapturingShell(
            mapOf(
                ("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" },
                ("git" to listOf("branch", "-r")) to {
                    """
                        origin/HEAD -> origin/main
                        origin/main
                        origin/update/scalatest-3.2.9
                        origin/update/handlebars-4.1.2
                        origin/update/log4j-core-2.13.3
                    """.trimIndent()
                }
            )
        )
        val publisher = GitHubPullRequestPublisher(shell, Settings())
        val manifests = listOf(createTestManifest(), createTestManifest())

        publisher.publish(
            BuildpackUpdate(
                manifests,
                VersionedBuildpack(
                    "test/buildpack",
                    "https://a.host/test/buildpack",
                    SemanticVersion("2.0.4"),
                    GitTag("v2.0.4")
                ),
                BuildpackVersion(SemanticVersion("2.3.6"), GitTag("v2.3.6"))
            )
        )

        shell.commands shouldBeEqualTo listOf(
            "git" to listOf("remote", "prune", "origin"),
            "git" to listOf("fetch", "--prune", "--prune-tags"),
            "git" to listOf("config", "user.name", "Buildpack Update Action"),
            "git" to listOf("config", "user.email", "do_not_reply@springernature.com"),
            "git" to listOf("rev-parse", "--abbrev-ref", "HEAD"),
            "git" to listOf("branch", "-r"),
            "git" to listOf("checkout", "-B", "buildpack-update/test-buildpack-2.3.6", "--quiet"),
            "git" to listOf(
                "commit", "-a", "--quiet",
                "-m", "Update test/buildpack to 2.3.6",
                "--author", "Buildpack Update Action <do_not_reply@springernature.com>"
            ),
            "git" to listOf("push", "--set-upstream", "origin", "buildpack-update/test-buildpack-2.3.6"),
            "hub" to listOf(
                "pull-request", "--push",
                "--message", "Update test/buildpack to 2.3.6\n\n"
                        + "Update test/buildpack from 2.0.4 to 2.3.6.\n\n"
                        + "* [Release Notes](https://github.com/test/buildpack/releases/tag/v2.3.6)\n"
                        + "* [Diff](https://github.com/test/buildpack/compare/v2.0.4...v2.3.6)",
                "--base", "base-branch", "--labels", "buildpack-update"
            ),
            "git" to listOf("switch", "base-branch")
        )
    }

    @Test
    fun `the pull request is created correctly when the tag does not use a 'v' prefix`() {
        val shell = CapturingShell(
            mapOf(
                ("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" },
                ("git" to listOf("branch", "-r")) to {
                    """
                        origin/HEAD -> origin/main
                        origin/main
                        origin/update/scalatest-3.2.9
                        origin/update/handlebars-4.1.2
                        origin/update/log4j-core-2.13.3
                    """.trimIndent()
                }
            )
        )
        val publisher = GitHubPullRequestPublisher(shell, Settings())
        val manifest = createTestManifest()

        publisher.publish(
            BuildpackUpdate(
                listOf(manifest),
                VersionedBuildpack(
                    "test/buildpack",
                    "https://a.host/test/buildpack",
                    SemanticVersion("2.0.5"),
                    GitTag("2.0.5")
                ),
                BuildpackVersion(SemanticVersion("2.3.6"), GitTag("2.3.6"))
            )
        )

        shell.commands shouldContainAll listOf(
            "git" to listOf("checkout", "-B", "buildpack-update/test-buildpack-2.3.6", "--quiet"),
            "git" to listOf(
                "commit", "-a", "--quiet",
                "-m", "Update test/buildpack to 2.3.6",
                "--author", "Buildpack Update Action <do_not_reply@springernature.com>"
            ),
            "git" to listOf("push", "--set-upstream", "origin", "buildpack-update/test-buildpack-2.3.6"),
            "hub" to listOf(
                "pull-request", "--push",
                "--message", "Update test/buildpack to 2.3.6\n\n"
                        + "Update test/buildpack from 2.0.5 to 2.3.6.\n\n"
                        + "* [Release Notes](https://github.com/test/buildpack/releases/tag/2.3.6)\n"
                        + "* [Diff](https://github.com/test/buildpack/compare/2.0.5...2.3.6)",
                "--base", "base-branch", "--labels", "buildpack-update"
            )
        )
    }

    @Test
    fun `clean up old pull requests when one for a newer version is created`() {
        val shell = CapturingShell(
            mapOf(
                ("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" },
                ("git" to listOf("branch", "-r")) to {
                    """
                        origin/HEAD -> origin/main
                        origin/main
                        origin/update/scalatest-3.2.9
                        origin/buildpack-update/test-buildpack-2.3.5
                        origin/update/handlebars-4.1.2
                        origin/buildpack-update/test-buildpack-2.2.1
                        origin/buildpack-update/test-buildpack-2.4.0
                        origin/update/log4j-core-2.13.3
                    """.trimIndent()
                }
            )
        )
        val publisher = GitHubPullRequestPublisher(shell, Settings())
        val manifest = createTestManifest()

        publisher.publish(
            BuildpackUpdate(
                listOf(manifest),
                VersionedBuildpack(
                    "test/buildpack",
                    "https://a.host/test/buildpack",
                    SemanticVersion("2.0.4"),
                    GitTag("v2.0.4")
                ),
                BuildpackVersion(SemanticVersion("2.3.6"), GitTag("v2.3.6"))
            )
        )

        shell.commands shouldBeEqualTo listOf(
            "git" to listOf("remote", "prune", "origin"),
            "git" to listOf("fetch", "--prune", "--prune-tags"),
            "git" to listOf("config", "user.name", "Buildpack Update Action"),
            "git" to listOf("config", "user.email", "do_not_reply@springernature.com"),
            "git" to listOf("rev-parse", "--abbrev-ref", "HEAD"),
            "git" to listOf("branch", "-r"),
            "git" to listOf("checkout", "-B", "buildpack-update/test-buildpack-2.3.6", "--quiet"),
            "git" to listOf(
                "commit", "-a", "--quiet",
                "-m", "Update test/buildpack to 2.3.6",
                "--author", "Buildpack Update Action <do_not_reply@springernature.com>"
            ),
            "git" to listOf("push", "--set-upstream", "origin", "buildpack-update/test-buildpack-2.3.6"),
            "hub" to listOf(
                "pull-request", "--push",
                "--message", "Update test/buildpack to 2.3.6\n\n"
                        + "Update test/buildpack from 2.0.4 to 2.3.6.\n\n"
                        + "* [Release Notes](https://github.com/test/buildpack/releases/tag/v2.3.6)\n"
                        + "* [Diff](https://github.com/test/buildpack/compare/v2.0.4...v2.3.6)",
                "--base", "base-branch", "--labels", "buildpack-update"
            ),
            "git" to listOf("push", "origin", ":buildpack-update/test-buildpack-2.3.5"),
            "git" to listOf("push", "origin", ":buildpack-update/test-buildpack-2.2.1"),
            "git" to listOf("switch", "base-branch")
        )
    }

    @Test
    fun `the current directory is dropped from manifest paths in messages`() {
        val shell = CapturingShell(
            mapOf(
                ("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" },
                ("git" to listOf("branch", "-r")) to {
                    """
                        origin/HEAD -> origin/main
                        origin/main
                        origin/update/scalatest-3.2.9
                        origin/buildpack-update/test-buildpack-2.3.5
                        origin/update/handlebars-4.1.2
                        origin/buildpack-update/test-buildpack-2.2.1
                        origin/buildpack-update/test-buildpack-2.4.0
                        origin/update/log4j-core-2.13.3
                    """.trimIndent()
                }
            )
        )
        val publisher = GitHubPullRequestPublisher(shell, Settings())
        val manifest = createTestManifest(File("./local-test-manifest.yml"))
        manifest.toString() shouldBe "./local-test-manifest.yml"

        try {
            publisher.publish(
                BuildpackUpdate(
                    listOf(manifest),
                    VersionedBuildpack(
                        "test/buildpack",
                        "https://a.host/test/buildpack",
                        SemanticVersion("2.0.4"),
                        GitTag("v2.0.4")
                    ),
                    BuildpackVersion(SemanticVersion("2.3.6"), GitTag("v2.3.6"))
                )
            )

            shell.commands shouldContain
                    ("hub" to listOf(
                        "pull-request",
                        "--push",
                        "--message",
                        "Update test/buildpack to 2.3.6\n\n"
                                + "Update test/buildpack from 2.0.4 to 2.3.6.\n\n"
                                + "* [Release Notes](https://github.com/test/buildpack/releases/tag/v2.3.6)\n"
                                + "* [Diff](https://github.com/test/buildpack/compare/v2.0.4...v2.3.6)",
                        "--base",
                        "base-branch",
                        "--labels",
                        "buildpack-update"
                    ))
        } finally {
            manifest.delete()
        }
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
                listOf(manifest),
                VersionedBuildpack(
                    "test/buildpack",
                    "https://a.host/test/buildpack",
                    SemanticVersion("2.0.4"),
                    GitTag("v2.0.4")
                ),
                BuildpackVersion(SemanticVersion("2.3.6"), GitTag("v2.3.6"))
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

    @Test
    fun `the manifests should be updated to point at the new version of the buildpack when more than one manifest is provided`() {
        val shell = CapturingShell(
            mapOf(
                ("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to { "base-branch" }
            )
        )
        val publisher = GitHubPullRequestPublisher(shell, Settings())
        val manifests = listOf(createTestManifest(name = "dummy1"), createTestManifest(name = "dummy2"))

        publisher.publish(
            BuildpackUpdate(
                manifests,
                VersionedBuildpack(
                    "test/buildpack",
                    "https://a.host/test/buildpack",
                    SemanticVersion("2.0.4"),
                    GitTag("v2.0.4")
                ),
                BuildpackVersion(SemanticVersion("2.3.6"), GitTag("v2.3.6"))
            )
        )

        manifests.forEachIndexed { index, manifest ->
            manifest.readText() shouldBeEqualTo """
            ---
            applications:
            - name: dummy${index + 1}
              instances: 1
              health-check-type: process
              no-route: true
              buildpacks:
              - https://a.host/test/buildpack#v2.3.6
        """.trimIndent()
        }
    }

    private fun createTestManifest(
        file: File = File.createTempFile(
            "github-pull-request-publisher-test-manifest",
            ".yml"
        ),
        name: String = "dummy-manifest-for-testing"
    ): File =
        file.also {
            it.deleteOnExit()
            it.writeText(
                """
                    ---
                    applications:
                    - name: $name
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
