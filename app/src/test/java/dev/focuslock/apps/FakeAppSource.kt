package dev.focuslock.apps

class FakeAppSource(
    private val installed: List<AppEntry> = emptyList(),
    private val protected: Set<String> = emptySet(),
) : AppSource {
    override fun launchableApps(): List<AppEntry> = installed
    override fun label(packageName: String): String? = installed.firstOrNull { it.packageName == packageName }?.label
    override fun protectedPackages(): Set<String> = protected
}
