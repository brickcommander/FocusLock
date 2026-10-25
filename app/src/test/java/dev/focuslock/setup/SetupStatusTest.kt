package dev.focuslock.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class SetupStatusTest {

    @Test
    fun `complete only when all three true`() {
        val bools = listOf(false, true)

        for (service in bools) for (admin in bools) for (pin in bools) {
            val status = SetupStatus(blockerServiceEnabled = service, deviceAdminActive = admin, pinSet = pin)

            assertEquals("service=$service admin=$admin pin=$pin", service && admin && pin, status.isComplete)
        }
    }
}
