package dev.focuslock.policy

interface PolicyController {
    fun isDeviceOwner(): Boolean
    fun clearDeviceOwner()
}
