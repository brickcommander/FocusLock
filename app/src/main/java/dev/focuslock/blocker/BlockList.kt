package dev.focuslock.blocker

import dev.focuslock.BuildConfig

object BlockList {
    const val SELF = "dev.focuslock"
    const val CHROME = "com.android.chrome"
    const val BRAVE = "com.brave.browser"

    /** Browsers whose URL bar is read, with their URL-bar view IDs (see BrowserUrlReader spike). */
    val browsers: Map<String, List<String>> = mapOf(
        CHROME to listOf("com.android.chrome:id/url_bar"),
        BRAVE to listOf("com.brave.browser:id/url_bar"),
    )

    /** Default apps: empty. The user builds their own list. Debug keeps the dummy target for testing. */
    val apps: Set<String> = if (BuildConfig.DEBUG) setOf("dev.focuslock.dummytarget") else emptySet()

    /** Default sites: empty. The user adds their own. */
    val sites: Set<String> = emptySet()

    val neverBlock: Set<String> = setOf(SELF, "com.android.vending")

    /** Packages whose screens SettingsGuard inspects (observed on AOSP; One UI added in Step 8). */
    val settingsPackages: Set<String> = setOf("com.android.settings")

    /** The service only receives events from these packages (set at runtime via setServiceInfo). */
    fun watchedPackages(rules: BlockRules, limited: Set<String> = emptySet()): Set<String> =
        rules.apps + limited + browsers.keys + settingsPackages
}
