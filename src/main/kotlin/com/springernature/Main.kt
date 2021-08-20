package com.springernature

import java.io.File

fun main(args: Array<String>) {

    println("Hello from Kotlin!")
    println("This is the content of the current directory:")

    // using extension function walk
    File(".").walk().forEach {
        println(it)
    }

}
