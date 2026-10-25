package dev.focuslock.blocker

/** The user's block list: app package names and website host suffixes ("youtube.com"). */
data class BlockRules(val apps: Set<String>, val sites: Set<String>) {
    companion object {
        val DEFAULT = BlockRules(apps = BlockList.apps, sites = BlockList.sites)
    }
}
