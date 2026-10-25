package dev.focuslock.blocker

/** In-memory BlockRulesStore for JVM tests. Like SharedPreferences, listeners only hear real changes. */
class FakeBlockRulesStore(initial: BlockRules = BlockRules.DEFAULT) : BlockRulesStoreContract {
    private val listeners = mutableSetOf<(BlockRules) -> Unit>()

    override var rules: BlockRules = initial
        set(value) {
            if (field == value) return
            field = value
            listeners.toList().forEach { it(value) }
        }

    override fun addListener(listener: (BlockRules) -> Unit) {
        listeners.add(listener)
    }

    override fun removeListener(listener: (BlockRules) -> Unit) {
        listeners.remove(listener)
    }
}
