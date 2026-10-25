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
 * opens the G2 DeviceAdminAdd screen instead. The activation screen (setup) is titled "Activate
 * device admin app?" and says "Activating this admin app will allow…", so it stays allowed.
 *
 * One UI (S25) texts are NOT recorded. Its device-admin page reached from the notification shade
 * was not blocked by the exact AOSP sentence, so the G2 rules match words instead of sentences.
 * The activation page ("Activating this admin app will allow…") contains neither "active" nor
 * "deactivate", so setup and the "Fix" button keep working. English only (phone language = English).
 *
 * Matching: a needle matches when a screen text equals it, ignoring case and outer whitespace.
 * Exact matching keeps lists that merely mention FocusLock (apps list, accessibility list) allowed.
 */
class SettingsGuard(rules: List<GuardRule> = GuardRules.default) {

    private val normalizedRules = rules.map { rule ->
        GuardRule(
            name = rule.name,
            allOf = rule.allOf.mapTo(HashSet(), ::normalize),
            anyOf = rule.anyOf.mapTo(HashSet(), ::normalize),
            oneTextContainsAll = rule.oneTextContainsAll.mapTo(HashSet(), ::normalize),
        )
    }

    fun isDangerous(screenTexts: List<String>): Boolean {
        if (screenTexts.isEmpty()) return false
        val texts = screenTexts.mapTo(HashSet(), ::normalize)
        return normalizedRules.any { rule ->
            texts.containsAll(rule.allOf) &&
                (rule.anyOf.isEmpty() || rule.anyOf.any { it in texts }) &&
                (rule.oneTextContainsAll.isEmpty() || texts.any { text -> rule.oneTextContainsAll.all { it in text } })
        }
    }

    private fun normalize(text: String): String = text.trim().lowercase()
}

/**
 * Matches when the screen contains ALL of `allOf`, ANY of `anyOf`, and one text that contains
 * every word of `oneTextContainsAll` (all case-insensitive; empty sets always match).
 */
data class GuardRule(
    val name: String,
    val allOf: Set<String>,
    val anyOf: Set<String>,
    val oneTextContainsAll: Set<String> = emptySet(),
)

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
        GuardRule("G2 device-admin-deactivate", allOf = setOf(APP), anyOf = emptySet(), oneTextContainsAll = setOf("deactivate")),
        GuardRule(
            "G2 device-admin-status",
            allOf = emptySet(),
            anyOf = emptySet(),
            oneTextContainsAll = setOf(APP, "admin", "active"),
        ),
        GuardRule("G2 device-admin-list", allOf = setOf(APP), anyOf = setOf("Device admin apps", "Device administrators")),
        GuardRule("G3 accessibility-detail", allOf = setOf(APP), anyOf = setOf("Use FocusLock", "FocusLock shortcut")),
        GuardRule("G3 accessibility-off-dialog", allOf = setOf("Turn off FocusLock?"), anyOf = setOf("Turn off")),
        GuardRule(
            "G4 date-time",
            allOf = setOf("Date & time"),
            anyOf = setOf("Automatic date and time", "Use 24-hour format", "12-hour / 24-hour format", "Time zone change"),
        ),
    )
}
