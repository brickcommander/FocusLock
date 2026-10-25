package dev.focuslock.setup

/** Parses Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES ("pkg/cls:pkg/.cls:…"). */
object EnabledServicesParser {

    fun isEnabled(setting: String?, packageName: String, className: String): Boolean =
        setting.orEmpty().split(':').any { entry ->
            val pkg = entry.trim().substringBefore('/', missingDelimiterValue = "")
            val cls = entry.trim().substringAfter('/', missingDelimiterValue = "")
            val fullClass = if (cls.startsWith('.')) pkg + cls else cls
            pkg.isNotEmpty() && pkg == packageName && fullClass == className
        }
}
