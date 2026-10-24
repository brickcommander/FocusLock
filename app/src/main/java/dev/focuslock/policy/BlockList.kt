package dev.focuslock.policy

import dev.focuslock.BuildConfig

object BlockList {
    const val CHROME = "com.android.chrome"

    val sites: List<String> = listOf("youtube.com", "youtu.be", "reddit.com", "redd.it", "instagram.com")

    val apps: List<String> = buildList {
        add("com.google.android.youtube")
        add("com.reddit.frontpage")
        add("com.instagram.android")
        add("com.sec.android.app.sbrowser")   // Samsung Internet
        add("org.mozilla.firefox")
        add("com.brave.browser")
        if (BuildConfig.DEBUG) add("dev.focuslock.dummytarget")
    }

    val neverTouch: Set<String> = setOf(
        "dev.focuslock",
        "com.android.settings",
        "com.android.vending",
    )
}
