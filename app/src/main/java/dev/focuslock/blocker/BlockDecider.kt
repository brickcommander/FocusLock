package dev.focuslock.blocker

class BlockDecider(private val settingsGuard: SettingsGuard) {

    /** Reading the URL bar costs IPC, so only do it when the answer can matter. */
    fun needsUrlBar(pkg: String, isLocked: Boolean): Boolean = isLocked && pkg == BlockList.CHROME

    /** Collecting Settings texts walks the view tree, so only do it when the answer can matter. */
    fun needsScreenTexts(pkg: String, isLocked: Boolean): Boolean = isLocked && pkg in BlockList.settingsPackages

    fun decide(event: ScreenEvent, isLocked: Boolean): Decision {
        if (!isLocked) return Decision.Allow

        val pkg = event.packageName
        if (pkg in BlockList.neverBlock) return Decision.Allow
        if (pkg in BlockList.apps) return Decision.BlockApp(pkg)

        if (pkg == BlockList.CHROME) {
            val host = event.urlBarText?.let(HostMatcher::extractHost)
            if (host != null && HostMatcher.isBlockedHost(host)) return Decision.BlockSite(host)
        }

        if (pkg in BlockList.settingsPackages && settingsGuard.isDangerous(event.screenTexts)) {
            return Decision.BlockSettings
        }

        return Decision.Allow
    }
}
