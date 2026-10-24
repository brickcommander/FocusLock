package dev.focuslock.policy

data class BrowserPolicy(
    val urlBlocklist: List<String>,
    val incognitoModeAvailability: Int,   // 1 = disabled
) {
    companion object {
        const val INCOGNITO_DISABLED = 1

        fun locked(): BrowserPolicy = BrowserPolicy(
            urlBlocklist = BlockList.sites,
            incognitoModeAvailability = INCOGNITO_DISABLED,
        )
    }
}
