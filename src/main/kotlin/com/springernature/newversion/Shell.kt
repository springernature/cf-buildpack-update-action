package com.springernature.newversion

import com.lordcodes.turtle.ShellScript
import java.io.File

interface Shell {
    fun run(workingDirectory: File? = null, script: ShellScript.() -> String): String
}

class TurtleShell : Shell {
    override fun run(workingDirectory: File?, script: ShellScript.() -> String): String =
        com.lordcodes.turtle.shellRun(workingDirectory, script)
}
