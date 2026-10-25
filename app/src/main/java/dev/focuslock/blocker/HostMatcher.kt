package dev.focuslock.blocker

object HostMatcher {

    private val SCHEME = Regex("^[a-z][a-z0-9+.-]*://")

    /** "https://m.youtube.com/watch?v=1" → "m.youtube.com"; "best pizza" → null */
    fun extractHost(urlBarText: String): String? {
        val text = urlBarText.trim().lowercase()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return null

        val authority = text
            .replaceFirst(SCHEME, "")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
        val host = authority
            .substringAfterLast('@')
            .substringBefore(':')
            .trimEnd('.')

        return if (host.contains('.') && !host.startsWith('.')) host else null
    }

    fun isBlockedHost(host: String, blocked: Set<String> = BlockList.sites): Boolean =
        blocked.any { suffix -> host == suffix || host.endsWith(".$suffix") }
}
