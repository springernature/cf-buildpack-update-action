package com.springernature.newversion

import com.lordcodes.turtle.shellRun
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

interface Shell {
    fun run(workingDirectory: File? = null, script: Script.() -> String): String
}

interface Script {
    fun command(
        command: String,
        arguments: List<String> = listOf()
    ): String
}

class TurtleShell : Shell {
    override fun run(workingDirectory: File?, script: Script.() -> String): String =
        TurtleScript(workingDirectory).script()
}

class TurtleScript(private val workingDirectory: File?) : Script {
    override fun command(command: String, arguments: List<String>): String {
        return shellRun(workingDirectory) {
            LOG.debug("Shell: {} {}", command, arguments.joinToString(" ") { """"$it"""" })
            command(command, arguments)
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(TurtleScript::class.java)
    }
}
