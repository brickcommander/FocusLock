package dev.focuslock.blocker

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Reads the page address from a supported browser's URL bar (see BlockList.browsers).
 *
 * Spike (emulator API 37, Chrome 145.0.7632.218, 2026-10-25), `uiautomator dump`:
 * | Screen                          | resource-id                      | text                         | focused |
 * |---------------------------------|----------------------------------|------------------------------|---------|
 * | Normal tab, youtube.com loading | com.android.chrome:id/url_bar    | "youtube.com"                | false   |
 * | Normal tab after redirect       | com.android.chrome:id/url_bar    | "m.youtube.com"              | false   |
 * | Normal tab, reddit.com warning  | com.android.chrome:id/url_bar    | "https://reddit.com"         | false   |
 * | Bar tapped, nothing typed       | com.android.chrome:id/url_bar    | "Search Google or type URL"  | true    |
 * | Bar while typing                | com.android.chrome:id/url_bar    | "reddit.co" (typed so far)   | true    |
 * | Incognito new tab page          | com.android.chrome:id/url_bar    | "Search Google or type URL"  | false   |
 * | Incognito, reddit.com           | com.android.chrome:id/url_bar    | "reddit.com"                 | false   |
 * | Custom Tab (CustomTabActivity)  | com.android.chrome:id/url_bar    | "m.youtube.com"              | false   |
 * Incognito and Custom Tabs use the same ID, so one entry covers all of them.
 *
 * Spike (emulator API 37, Brave 1.98.60 from GitHub releases, 2026-10-25), `uiautomator dump`:
 * | Screen                          | resource-id                      | text                         | focused |
 * |---------------------------------|----------------------------------|------------------------------|---------|
 * | Normal tab, youtube.com         | com.brave.browser:id/url_bar     | "m.youtube.com"              | false   |
 * | Normal tab, reddit.com          | com.brave.browser:id/url_bar     | "reddit.com"                 | false   |
 * | Bar tapped, nothing typed       | com.brave.browser:id/url_bar     | "Search Brave or type URL"   | true    |
 * | Bar while typing                | com.brave.browser:id/url_bar     | "reddit.co" (typed so far)   | true    |
 * Brave private tabs were not reachable on the emulator (needs the phone).
 *
 * A focused bar holds what the user is typing, not the open page, so it is skipped; otherwise
 * typing "youtube.com alternatives" would be blocked half-way. The page is checked once loaded.
 *
 * Known gap (accepted): the blocked tab stays open in the browser. Re-opening the browser blocks
 * again until the user closes that tab.
 */
object BrowserUrlReader {

    fun read(root: AccessibilityNodeInfo?, pkg: String): String? {
        val urlBarIds = BlockList.browsers[pkg] ?: return null
        if (root == null || root.packageName?.toString() != pkg) return null
        for (id in urlBarIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            val text = nodes.firstOrNull { !it.isFocused && !it.text.isNullOrBlank() }?.text?.toString()
            recycle(nodes)
            if (text != null) return text
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun recycle(nodes: List<AccessibilityNodeInfo>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) nodes.forEach { it.recycle() }
    }
}
