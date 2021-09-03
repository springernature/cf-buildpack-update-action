package com.springernature.newversion

import com.lordcodes.turtle.GitCommands
import com.lordcodes.turtle.shellRun

val gitEmail: String = System.getenv("AUTHOR_EMAIL") ?: "do_not_reply@springernature.com"
val gitName: String = System.getenv("AUTHOR_NAME") ?: "buildpack update action"

fun createPullRequest(
    branchName: String,
    commitMessage: String,
    prMessage: String,
    makeChanges: () -> Unit
) {
    val baseBranchName = getBaseBranch()
    gitInit()
    try {
        if (branchExistsAlready(branchName)) {
            return
        }
        createBranchIfMissing(branchName)
        switchToBranch(branchName)
        makeChanges()
        commitChanges(commitMessage, gitName, gitEmail)
        createPullRequest(branchName, prMessage)
    } finally {
        switchToBranch(baseBranchName)
    }
}

private fun getBaseBranch(): String {
    return shellRun {
        git.currentBranch()
    }
}

private fun gitInit() {
    shellRun {
        command("git", listOf("remote", "prune", "origin"))
        command("git", listOf("fetch", "--prune", "--prune-tags"))
    }
}

private fun branchExistsAlready(name: String) =
    try {
        shellRun {
            git.gitCommand(listOf("switch", name))
        }
        true
    } catch (e: Exception) {
        false
    }

private fun createBranchIfMissing(name: String) {
    shellRun {
        println("creating branch $name")
        git.checkout(name, true)
    }
}

private fun switchToBranch(name: String) {
    shellRun {
        git.gitCommand(listOf("switch", name))
    }
}

private fun commitChanges(message: String, name: String, email: String) {
    println("commitChanges")
    shellRun {
        git.commit(message, name, email)
    }
}

private fun GitCommands.commit(message: String, name: String, email: String): String {
    return gitCommand(
        listOf(
            "commit",
            "--message", message,
            "--author", "$name <$email>",
        )
    )
}

private fun createPullRequest(name: String, prMessage: String) {
    println("createPullRequest")
    // https://hub.github.com/hub-pull-request.1.html
    shellRun {
        command(
            "hub",
            listOf(
                "pull-request",
                "--push",
                "--message=$prMessage",
                "--base=$name",
                "--labels=buildpack-update"
            )
        )
    }
}
