package com.springernature.newversion

import org.amshove.kluent.shouldBe
import org.junit.jupiter.api.Test

class MainTest {

    @Test
    fun `exit status for a successful check should be 0`() {
        val exitStatus = SuccessfulChecks(emptyList()).exitStatus()
        exitStatus shouldBe 0
    }

    @Test
    fun `exit status for a failed check should be 1`() {
        val exitStatus = FailedChecks(emptyList(), emptyMap()).exitStatus()
        exitStatus shouldBe 1
    }

}