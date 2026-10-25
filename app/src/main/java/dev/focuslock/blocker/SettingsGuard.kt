package dev.focuslock.blocker

/**
 * Decides whether a Settings screen could disable or remove FocusLock (while locked).
 *
 * Spike (emulator "FocusLock-Test", AOSP API 37, 2026-10-25), all windows from package
 * com.android.settings, texts AS SEEN BY THE SERVICE (debug log "settings texts=[...]"):
 * | #   | Screen                                 | Distinctive texts (exact)                                          |
 * |-----|----------------------------------------|--------------------------------------------------------------------|
 * | G1  | App info → FocusLock                   | "FocusLock", "Archive", "Uninstall", "Force stop", "App info"      |
 * | G1  | App info → FocusLock → Storage & cache | "Storage", "FocusLock", "Space used", "User data", "Cache"         |
 * | G2  | Device admin apps → FocusLock          | "Device admin app", "FocusLock", ACTIVE_ADMIN_TEXT                 |
 * | G2  | App info → Uninstall (admin active)    | same screen as above (DeviceAdminAdd)                              |
 * | G3  | Accessibility → FocusLock              | "FocusLock", "Use FocusLock", "FocusLock shortcut", "App info"     |
 * | G3  | "Use FocusLock" off → dialog           | "Turn off FocusLock?", "Keep on", "Turn off"                       |
 * | G4  | Date & time                            | "Date & time", "Time zone", "Use 24-hour format", "Time zone change"|
 * | ok  | Apps → All apps                        | "All apps", "FocusLock", "39.57 MB", other app names               |
 * | ok  | Accessibility main list                | "Accessibility", "Downloaded apps", "FocusLock", "Off"/"On"        |
 * | ok  | Device admin apps list                 | "Device admin" (list items are hidden from the service)            |
 * | ok  | Settings home                          | "Storage", "System", "Languages, gestures, time", ...              |
 * Hidden from the service but visible to uiautomator: "Deactivate this device admin app",
 * "Deactivate & uninstall", "Cancel", "Clear storage", "Clear cache", "Automatic date and time",
 * "Automatic time zone". Those needles are kept in case another Settings build shows them.
 * Notes: with the admin active, AOSP greys out Force stop, Archive and Clear storage; Uninstall
 * opens the G2 DeviceAdminAdd screen instead. The activation screen (setup) says "Activating this
 * admin app will allow…", so ACTIVE_ADMIN_TEXT keeps it allowed.
 *
 * One UI (S25) wordings and packages are NOT recorded yet: they need the same dump on the phone
 * (Step 8) and extra rules here. English only for now (phone language = English).
 *
 * Matching: a needle matches when a screen text equals it, ignoring case and outer whitespace.
 * Exact matching keeps lists that merely mention FocusLock (apps list, accessibility list) allowed.
 */
class SettingsGuard(rules: List<GuardRule> = GuardRules.default) {

    private val normalizedRules = rules.map { rule ->
        rule.allOf.mapTo(HashSet(), ::normalize) to rule.anyOf.mapTo(HashSet(), ::normalize)
    }

    fun isDangerous(screenTexts: List<String>): Boolean {
        if (screenTexts.isEmpty()) return false
        val texts = screenTexts.mapTo(HashSet(), ::normalize)
        return normalizedRules.any { (allOf, anyOf) ->
            texts.containsAll(allOf) && (anyOf.isEmpty() || anyOf.any { it in texts })
        }
    }

    private fun normalize(text: String): String = text.trim().lowercase()
}

/** Matches when the screen contains ALL of `allOf` and ANY of `anyOf` (case-insensitive). */
data class GuardRule(val name: String, val allOf: Set<String>, val anyOf: Set<String>)

object GuardRules {
    private const val APP = "FocusLock"
    const val ACTIVE_ADMIN_TEXT = "This admin app is active and allows the app FocusLock to perform the following operations:"

    val default: List<GuardRule> = listOf(
        GuardRule("G1 app-info", allOf = setOf(APP), anyOf = setOf("Force stop", "Uninstall", "Archive")),
        GuardRule(
            "G1 app-storage",
            allOf = setOf(APP),
            anyOf = setOf("Clear storage", "Clear cache", "User data", "Space used"),
        ),
        GuardRule(
            "G2 device-admin-detail",
            allOf = setOf(APP),
            anyOf = setOf("Deactivate this device admin app", "Deactivate & uninstall", ACTIVE_ADMIN_TEXT),
        ),
        GuardRule("G3 accessibility-detail", allOf = setOf(APP), anyOf = setOf("Use FocusLock", "FocusLock shortcut")),
        GuardRule("G3 accessibility-off-dialog", allOf = setOf("Turn off FocusLock?"), anyOf = setOf("Turn off")),
        GuardRule(
            "G4 date-time",
            allOf = setOf("Date & time"),
            anyOf = setOf("Automatic date and time", "Use 24-hour format", "12-hour / 24-hour format", "Time zone change"),
        ),
    )
}
