package dev.focuslock.blocker

import dev.focuslock.lock.FakeLockStateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BlockListEditorTest {

    private lateinit var rulesStore: FakeBlockRulesStore
    private lateinit var lockStore: FakeLockStateStore
    private lateinit var editor: BlockListEditor

    @Before
    fun setUp() {
        rulesStore = FakeBlockRulesStore(BlockRules(apps = setOf(INSTAGRAM), sites = setOf("youtube.com")))
        lockStore = FakeLockStateStore()
        editor = BlockListEditor(rulesStore, lockStore, protectedPackages = { setOf(LAUNCHER, DIALER) })
    }

    @Test
    fun `addApp adds a valid package when unlocked`() {
        val result = editor.addApp("  com.zhiliaoapp.musically ")

        assertEquals(EditResult.OK, result)
        assertTrue("com.zhiliaoapp.musically" in rulesStore.rules.apps)
    }

    @Test
    fun `addApp is refused while locked and the list is unchanged`() {
        lockStore.isLocked = true

        val result = editor.addApp("com.zhiliaoapp.musically")

        assertEquals(EditResult.LOCKED, result)
        assertEquals(setOf(INSTAGRAM), rulesStore.rules.apps)
    }

    @Test
    fun `addApp rejects text that is not a package name`() {
        val inputs = listOf("", "instagram", "com..x", "1com.app", "com.app/", "com app.x")

        inputs.forEach { assertEquals(it, EditResult.INVALID, editor.addApp(it)) }
        assertEquals(setOf(INSTAGRAM), rulesStore.rules.apps)
    }

    @Test
    fun `addApp refuses self, play store, settings, launcher and dialer`() {
        val protected = listOf("dev.focuslock", "com.android.vending", "com.android.settings", LAUNCHER, DIALER)

        protected.forEach { assertEquals(it, EditResult.PROTECTED, editor.addApp(it)) }
        assertEquals(setOf(INSTAGRAM), rulesStore.rules.apps)
    }

    @Test
    fun `addApp reports a package that is already blocked`() {
        assertEquals(EditResult.ALREADY_THERE, editor.addApp(INSTAGRAM))
    }

    @Test
    fun `removeApp removes when unlocked`() {
        val result = editor.removeApp(INSTAGRAM)

        assertEquals(EditResult.OK, result)
        assertFalse(INSTAGRAM in rulesStore.rules.apps)
    }

    @Test
    fun `removeApp is refused while locked`() {
        lockStore.isLocked = true

        val result = editor.removeApp(INSTAGRAM)

        assertEquals(EditResult.LOCKED, result)
        assertTrue(INSTAGRAM in rulesStore.rules.apps)
    }

    @Test
    fun `addSite stores the normalized host`() {
        val result = editor.addSite("https://www.TikTok.com/foryou?x=1")

        assertEquals(EditResult.OK, result)
        assertTrue("tiktok.com" in rulesStore.rules.sites)
    }

    @Test
    fun `addSite rejects text that is not a website`() {
        val inputs = listOf("", "tiktok", "best pizza", "www.")

        inputs.forEach { assertEquals(it, EditResult.INVALID, editor.addSite(it)) }
        assertEquals(setOf("youtube.com"), rulesStore.rules.sites)
    }

    @Test
    fun `addSite reports a site that is already blocked`() {
        assertEquals(EditResult.ALREADY_THERE, editor.addSite("www.youtube.com"))
    }

    @Test
    fun `addSite and removeSite are refused while locked`() {
        lockStore.isLocked = true

        assertEquals(EditResult.LOCKED, editor.addSite("tiktok.com"))
        assertEquals(EditResult.LOCKED, editor.removeSite("youtube.com"))
        assertEquals(setOf("youtube.com"), rulesStore.rules.sites)
    }

    @Test
    fun `removeSite removes when unlocked`() {
        val result = editor.removeSite("youtube.com")

        assertEquals(EditResult.OK, result)
        assertTrue(rulesStore.rules.sites.isEmpty())
    }

    @Test
    fun `normalizeSite keeps subdomains other than www`() {
        assertEquals("m.facebook.com", BlockListEditor.normalizeSite("m.facebook.com"))
        assertEquals("x.com", BlockListEditor.normalizeSite("WWW.X.COM"))
        assertNull(BlockListEditor.normalizeSite("localhost"))
    }

    private companion object {
        const val INSTAGRAM = "com.instagram.android"
        const val LAUNCHER = "com.google.android.apps.nexuslauncher"
        const val DIALER = "com.google.android.dialer"
    }
}
