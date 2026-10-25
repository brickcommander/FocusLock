package dev.focuslock.blocker

/** What the Blocked screen says was blocked. */
object BlockedReason {

    private val appNames = mapOf(
        "com.instagram.android" to "Instagram",
        "com.instagram.lite" to "Instagram Lite",
        "com.google.android.youtube" to "YouTube",
        "com.reddit.frontpage" to "Reddit",
        "com.sec.android.app.sbrowser" to "Samsung Internet",
        "org.mozilla.firefox" to "Firefox",
        "com.brave.browser" to "Brave",
        "dev.focuslock.dummytarget" to "DummyTarget",
    )

    const val SETTINGS_SCREEN = "This Settings screen"

    /** @param appLabel looks up the installed app's name for apps the user added. */
    fun describe(decision: Decision, appLabel: (String) -> String? = { null }): String? = when (decision) {
        Decision.Allow -> null
        is Decision.BlockApp -> appNames[decision.pkg] ?: appLabel(decision.pkg) ?: decision.pkg
        is Decision.BlockSite -> decision.host
        Decision.BlockSettings -> SETTINGS_SCREEN
    }
}
