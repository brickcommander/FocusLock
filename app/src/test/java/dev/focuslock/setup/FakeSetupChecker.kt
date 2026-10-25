package dev.focuslock.setup

class FakeSetupChecker(
    var serviceEnabled: Boolean = true,
    var adminActive: Boolean = true,
    private val onDeactivateAdmin: () -> Unit = {},
) : SetupChecker {
    var deactivateAdminCalls = 0
        private set

    override fun isBlockerServiceEnabled(): Boolean = serviceEnabled
    override fun isDeviceAdminActive(): Boolean = adminActive

    override fun deactivateAdmin() {
        deactivateAdminCalls++
        onDeactivateAdmin()
        adminActive = false
    }
}
