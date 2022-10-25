package com.springernature.newversion

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class TurtleShellTest {

    @Test
    fun `executes a command and logs the output while the command is running`() {
        val shell = TurtleShell()

        val result = shell.run {
            command("sh", listOf("src/test/kotlin/com/springernature/newversion/turtleScriptTestScript.sh"))
        }

        result shouldBeEqualTo "1\n2"
    }
}