package dev.focuslock.blocker

class BlockDecider(private val settingsGuard: SettingsGuard) {

    /** Reading the URL bar costs IPC, so only do it when the answer can matter. */
    fun needsUrlBar(pkg: String, isLocked: Boolean): Boolean = isLocked && pkg in BlockList.browsers

    /** Collecting Settings texts walks the view tree, so only do it when the answer can matter. */
    fun needsScreenTexts(pkg: String, isLocked: Boolean): Boolean = isLocked && pkg in BlockList.settingsPackages

    fun decide(event: ScreenEvent, isLocked: Boolean, rules: BlockRules = BlockRules.DEFAULT): Decision {
        if (!isLocked) return Decision.Allow

        val pkg = event.packageName
        if (pkg in BlockList.neverBlock) return Decision.Allow
        if (pkg in rules.apps && pkg !in BlockList.settingsPackages) return Decision.BlockApp(pkg)

        if (pkg in BlockList.browsers) {
            val host = event.urlBarText?.let(HostMatcher::extractHost)
            if (host != null && HostMatcher.isBlockedHost(host, rules.sites)) return Decision.BlockSite(host)
        }

        if (pkg in BlockList.settingsPackages && settingsGuard.isDangerous(event.screenTexts)) {
            return Decision.BlockSettings
        }

        return Decision.Allow
    }
}
