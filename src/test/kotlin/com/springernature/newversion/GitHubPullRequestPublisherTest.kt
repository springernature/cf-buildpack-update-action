package com.springernature.newversion

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.File

class GitHubPullRequestPublisherTest {

    @Test
    fun `we don't create a new branch when the branch already exists`() {
        val shell = CapturingShell(mapOf(("git" to listOf("rev-parse", "--abbrev-ref", "HEAD")) to "base-branch"))
        val publisher = GitHubPullRequestPublisher(shell, Settings())

        publisher.publish(
            BuildpackUpdate(
                ".",
                VersionedBuildpack("test/buildpack", "https://a.host/path/buildpack", SemanticVersion("2.0.4")),
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

    private class CapturingShell(private val commandOutput: Map<Pair<String, List<String>>, String> = mapOf()) : Shell {
        val commands = mutableListOf<Pair<String, List<String>>>()
        override fun run(workingDirectory: File?, script: Script.() -> String): String {
            val capturingScript = CapturingScript(commandOutput)
            return capturingScript.script().also {
                commands.addAll(capturingScript.commands)
            }
        }
    }

    private class CapturingScript(private val commandOutput: Map<Pair<String, List<String>>, String> = mapOf()) :
        Script {
        val commands = mutableListOf<Pair<String, List<String>>>()
        override fun command(command: String, arguments: List<String>): String {
            commands.add(command to arguments)
            return commandOutput[command to arguments] ?: ""
        }
    }

}
