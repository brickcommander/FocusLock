package dev.focuslock.policy

import android.content.RestrictionEntry
import dev.focuslock.policy.RestrictionBundleMapper.KEY_INCOGNITO
import dev.focuslock.policy.RestrictionBundleMapper.KEY_URL_BLOCKLIST
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestrictionBundleMapperTest {

    private val policy = BrowserPolicy(
        urlBlocklist = listOf("youtube.com", "reddit.com"),
        incognitoModeAvailability = 1,
    )

    @Test
    fun `decideValueKind maps multi select to string array`() {
        assertEquals(ValueKind.STRING_ARRAY, RestrictionBundleMapper.decideValueKind(RestrictionEntry.TYPE_MULTI_SELECT))
    }

    @Test
    fun `decideValueKind maps string and choice to string`() {
        assertEquals(ValueKind.STRING, RestrictionBundleMapper.decideValueKind(RestrictionEntry.TYPE_STRING))
        assertEquals(ValueKind.STRING, RestrictionBundleMapper.decideValueKind(RestrictionEntry.TYPE_CHOICE))
    }

    @Test
    fun `decideValueKind maps integer to integer`() {
        assertEquals(ValueKind.INTEGER, RestrictionBundleMapper.decideValueKind(RestrictionEntry.TYPE_INTEGER))
    }

    @Test
    fun `decideValueKind marks boolean and bundle as unsupported`() {
        assertEquals(ValueKind.UNSUPPORTED, RestrictionBundleMapper.decideValueKind(RestrictionEntry.TYPE_BOOLEAN))
        assertEquals(ValueKind.UNSUPPORTED, RestrictionBundleMapper.decideValueKind(RestrictionEntry.TYPE_BUNDLE))
    }

    @Test
    fun `map with observed chrome types sends JSON string blocklist and string incognito`() {
        val schema = mapOf(
            KEY_URL_BLOCKLIST to RestrictionEntry.TYPE_STRING,
            KEY_INCOGNITO to RestrictionEntry.TYPE_CHOICE,
        )

        val mapped = RestrictionBundleMapper.map(policy, schema)

        assertEquals(
            listOf(
                MappedValue.StringValue(KEY_URL_BLOCKLIST, "[\"youtube.com\",\"reddit.com\"]"),
                MappedValue.StringValue(KEY_INCOGNITO, "1"),
            ),
            mapped,
        )
    }

    @Test
    fun `map with array and integer types sends string array and int`() {
        val schema = mapOf(
            KEY_URL_BLOCKLIST to RestrictionEntry.TYPE_MULTI_SELECT,
            KEY_INCOGNITO to RestrictionEntry.TYPE_INTEGER,
        )

        val mapped = RestrictionBundleMapper.map(policy, schema)

        assertEquals(
            listOf(
                MappedValue.StringArrayValue(KEY_URL_BLOCKLIST, listOf("youtube.com", "reddit.com")),
                MappedValue.IntValue(KEY_INCOGNITO, 1),
            ),
            mapped,
        )
    }

    @Test
    fun `map drops keys missing from schema`() {
        val mapped = RestrictionBundleMapper.map(policy, emptyMap())

        assertTrue(mapped.isEmpty())
    }

    @Test
    fun `toJsonArray escapes quotes and backslashes`() {
        val json = RestrictionBundleMapper.toJsonArray(listOf("a\"b", "c\\d"))

        assertEquals("[\"a\\\"b\",\"c\\\\d\"]", json)
    }
}
