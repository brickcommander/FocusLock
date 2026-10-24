package dev.focuslock.policy

import android.content.RestrictionEntry
import android.os.Bundle

/*
 * Observed Chrome schema (Chrome 145.0.7632.218, API 37 emulator, via "DEV: Log browser schema"):
 *   URLBlocklist              type=6 (TYPE_STRING) -> value is a JSON array string
 *   IncognitoModeAvailability type=2 (TYPE_CHOICE) -> value is a string ("1" = disabled)
 * Types are still read from the live schema on every call, so a Chrome update that changes them
 * is handled without a code change.
 */
enum class ValueKind { STRING_ARRAY, STRING, INTEGER, UNSUPPORTED }

sealed class MappedValue {
    abstract val key: String

    data class StringArrayValue(override val key: String, val value: List<String>) : MappedValue()
    data class StringValue(override val key: String, val value: String) : MappedValue()
    data class IntValue(override val key: String, val value: Int) : MappedValue()
}

object RestrictionBundleMapper {

    const val KEY_URL_BLOCKLIST = "URLBlocklist"
    const val KEY_INCOGNITO = "IncognitoModeAvailability"

    fun decideValueKind(type: Int): ValueKind = when (type) {
        RestrictionEntry.TYPE_MULTI_SELECT -> ValueKind.STRING_ARRAY
        RestrictionEntry.TYPE_STRING, RestrictionEntry.TYPE_CHOICE -> ValueKind.STRING
        RestrictionEntry.TYPE_INTEGER -> ValueKind.INTEGER
        else -> ValueKind.UNSUPPORTED
    }

    /** Keys missing from [schemaTypes] or with unsupported types are dropped, never guessed. */
    fun map(policy: BrowserPolicy, schemaTypes: Map<String, Int>): List<MappedValue> = buildList {
        schemaTypes[KEY_URL_BLOCKLIST]?.let { type ->
            when (decideValueKind(type)) {
                ValueKind.STRING_ARRAY ->
                    add(MappedValue.StringArrayValue(KEY_URL_BLOCKLIST, policy.urlBlocklist))
                ValueKind.STRING ->
                    add(MappedValue.StringValue(KEY_URL_BLOCKLIST, toJsonArray(policy.urlBlocklist)))
                else -> Unit
            }
        }
        schemaTypes[KEY_INCOGNITO]?.let { type ->
            when (decideValueKind(type)) {
                ValueKind.INTEGER ->
                    add(MappedValue.IntValue(KEY_INCOGNITO, policy.incognitoModeAvailability))
                ValueKind.STRING ->
                    add(MappedValue.StringValue(KEY_INCOGNITO, policy.incognitoModeAvailability.toString()))
                else -> Unit
            }
        }
    }

    fun toBundle(policy: BrowserPolicy, schema: List<RestrictionEntry>): Bundle {
        val bundle = Bundle()
        map(policy, schema.associate { it.key to it.type }).forEach { value ->
            when (value) {
                is MappedValue.StringArrayValue -> bundle.putStringArray(value.key, value.value.toTypedArray())
                is MappedValue.StringValue -> bundle.putString(value.key, value.value)
                is MappedValue.IntValue -> bundle.putInt(value.key, value.value)
            }
        }
        return bundle
    }

    fun toJsonArray(values: List<String>): String =
        values.joinToString(prefix = "[", postfix = "]", separator = ",") { value ->
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }
}
