package dev.focuslock.blocker

import dev.focuslock.BuildConfig

object BlockList {
    const val SELF = "dev.focuslock"
    const val CHROME = "com.android.chrome"

    val apps: Set<String> = buildSet {
        add("com.instagram.android")
        add("com.instagram.lite")
        add("com.google.android.youtube")
        add("com.reddit.frontpage")
        add("com.sec.android.app.sbrowser")   // Samsung Internet
        add("org.mozilla.firefox")
        add("com.brave.browser")
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
