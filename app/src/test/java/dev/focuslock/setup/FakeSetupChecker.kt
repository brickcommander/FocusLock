package dev.focuslock.setup

class FakeSetupChecker(
    var serviceEnabled: Boolean = true,
    var adminActive: Boolean = true,
) : SetupChecker {
    override fun isBlockerServiceEnabled(): Boolean = serviceEnabled
    override fun isDeviceAdminActive(): Boolean = adminActive
}
