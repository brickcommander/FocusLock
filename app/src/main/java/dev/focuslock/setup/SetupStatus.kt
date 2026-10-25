package dev.focuslock.setup

data class SetupStatus(
    val blockerServiceEnabled: Boolean,
    val deviceAdminActive: Boolean,
    val pinSet: Boolean,
) {
    val isComplete get() = blockerServiceEnabled && deviceAdminActive && pinSet
}
