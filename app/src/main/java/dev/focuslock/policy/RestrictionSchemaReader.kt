package dev.focuslock.policy

import android.content.Context
import android.content.RestrictionEntry
import android.content.RestrictionsManager
import android.util.Log

class RestrictionSchemaReader(context: Context) {

    private val restrictionsManager: RestrictionsManager =
        context.getSystemService(RestrictionsManager::class.java)

    fun read(pkg: String): List<RestrictionEntry> =
        restrictionsManager.getManifestRestrictions(pkg).orEmpty()

    fun logSchema(pkg: String) {
        val entries = read(pkg)
        Log.d(TAG, "Schema for $pkg: ${entries.size} entries")
        entries.forEach { Log.d(TAG, "${it.key} type=${it.type}") }
    }

    companion object {
        private const val TAG = "FocusLock"
    }
}
