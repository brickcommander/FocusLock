package dev.focuslock.policy

import android.os.UserManager
import dev.focuslock.BuildConfig

object BlockList {
    const val SELF = "dev.focuslock"
    const val CHROME = "com.android.chrome"

    val sites: List<String> = listOf("m.youtube.com", "youtube.com", "youtu.be", "reddit.com", "redd.it", "instagram.com")

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
        SELF,
        "com.android.settings",
        "com.android.vending",
    )

    val userRestrictions: List<String> = listOf(
        UserManager.DISALLOW_SAFE_BOOT,
        UserManager.DISALLOW_ADD_USER,
        UserManager.DISALLOW_CONFIG_DATE_TIME,
    )
}
