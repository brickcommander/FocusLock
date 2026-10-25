package dev.focuslock.blocker

/** Plain data taken from an AccessibilityEvent, so decisions stay testable on the JVM. */
data class ScreenEvent(
    val packageName: String,
    val urlBarText: String?,          // only for Chrome, else null
    val screenTexts: List<String>,    // only for Settings packages (Step 6), else empty
)
