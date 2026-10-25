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

    val apps: Set<String> = buildSet {
        add("com.instagram.android")
        add("com.instagram.lite")
        add("com.google.android.youtube")
        add("com.reddit.frontpage")
        add("com.sec.android.app.sbrowser")   // Samsung Internet
        add("org.mozilla.firefox")
        if (BuildConfig.DEBUG) add("dev.focuslock.dummytarget")
    }

    /** Host suffixes. "youtube.com" also blocks "m.youtube.com", "www.youtube.com". */
    val sites: Set<String> = setOf(
        "youtube.com", "youtu.be",
        "reddit.com", "redd.it",
        "instagram.com", "instagr.am",
    )

    val neverBlock: Set<String> = setOf(SELF, "com.android.vending")

    /** Packages whose screens SettingsGuard inspects (observed on AOSP; One UI added in Step 8). */
    val settingsPackages: Set<String> = setOf("com.android.settings")
}
