package dev.focuslock.policy

class FakePolicyController : PolicyController {

    var deviceOwner: Boolean = false
    val calls: MutableList<String> = mutableListOf()

    override fun isDeviceOwner(): Boolean {
        calls.add("isDeviceOwner")
        return deviceOwner
    }

    override fun clearDeviceOwner() {
        calls.add("clearDeviceOwner")
        deviceOwner = false
    }
}
