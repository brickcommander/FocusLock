package dev.focuslock.ui

import dev.focuslock.apps.AppEntry
import dev.focuslock.apps.FakeAppSource
import dev.focuslock.blocker.BlockListEditor
import dev.focuslock.blocker.BlockRules
import dev.focuslock.blocker.FakeBlockRulesStore
import dev.focuslock.lock.FakeLockStateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BlockListViewModelTest {

    private lateinit var rulesStore: FakeBlockRulesStore
    private lateinit var lockStore: FakeLockStateStore
    private lateinit var appSource: FakeAppSource

    @Before
    fun setUp() {
        rulesStore = FakeBlockRulesStore(BlockRules(apps = setOf(INSTAGRAM.packageName), sites = setOf("youtube.com")))
        lockStore = FakeLockStateStore()
        appSource = FakeAppSource(
            installed = listOf(INSTAGRAM, TIKTOK, LAUNCHER, SELF, PLAY_STORE),
            protected = setOf(LAUNCHER.packageName),
        )
    }

    private fun createViewModel(): BlockListViewModel {
        val editor = BlockListEditor(rulesStore, lockStore, appSource::protectedPackages)
        return BlockListViewModel(editor, lockStore, appSource)
    }

    @Test
    fun `state shows blocked apps with their labels and sorted sites`() {
        rulesStore.rules = BlockRules(apps = setOf(INSTAGRAM.packageName), sites = setOf("x.com", "reddit.com"))

        val vm = createViewModel()

        assertEquals(listOf(INSTAGRAM), vm.uiState.value.apps)
        assertEquals(listOf("reddit.com", "x.com"), vm.uiState.value.sites)
    }

    @Test
    fun `app that is not installed is shown by package name`() {
        rulesStore.rules = BlockRules(apps = setOf("com.gone.app"), sites = emptySet())

        val vm = createViewModel()

        assertEquals(listOf(AppEntry("com.gone.app", "com.gone.app")), vm.uiState.value.apps)
    }

    @Test
    fun `picker hides already blocked, protected, self and play store`() {
        val vm = createViewModel()

        vm.openPicker()

        assertTrue(vm.uiState.value.showPicker)
        assertEquals(listOf(TIKTOK), vm.uiState.value.pickerApps)
    }

    @Test
    fun `picking an app adds it and closes the picker`() {
        val vm = createViewModel()
        vm.openPicker()

        vm.addApp(TIKTOK.packageName)

        assertFalse(vm.uiState.value.showPicker)
        assertTrue(TIKTOK in vm.uiState.value.apps)
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun `typed invalid package shows a message and changes nothing`() {
        val vm = createViewModel()

        vm.addApp("tiktok")

        assertEquals("That isn't a package name (example: com.zhiliaoapp.musically)", vm.uiState.value.message)
        assertEquals(listOf(INSTAGRAM), vm.uiState.value.apps)
    }

    @Test
    fun `typed protected package shows a message`() {
        val vm = createViewModel()

        vm.addApp(LAUNCHER.packageName)

        assertEquals("This app can't be blocked (it's needed to use the phone)", vm.uiState.value.message)
    }

    @Test
    fun `adding a website shows the normalized host`() {
        val vm = createViewModel()

        vm.addSite("https://www.TikTok.com/foryou")

        assertEquals(listOf("tiktok.com", "youtube.com"), vm.uiState.value.sites)
    }

    @Test
    fun `removing a website and an app updates the lists`() {
        val vm = createViewModel()

        vm.removeSite("youtube.com")
        vm.removeApp(INSTAGRAM.packageName)

        assertTrue(vm.uiState.value.sites.isEmpty())
        assertTrue(vm.uiState.value.apps.isEmpty())
    }

    @Test
    fun `edits while locked are refused with a message`() {
        lockStore.isLocked = true
        val vm = createViewModel()

        vm.addSite("tiktok.com")
        vm.removeApp(INSTAGRAM.packageName)

        assertTrue(vm.uiState.value.isLocked)
        assertEquals("Unlock first to change the block list (not possible during a focus session)", vm.uiState.value.message)
        assertEquals(listOf("youtube.com"), vm.uiState.value.sites)
        assertEquals(listOf(INSTAGRAM), vm.uiState.value.apps)
    }

    @Test
    fun `refresh picks up a lock that happened elsewhere`() {
        val vm = createViewModel()

        lockStore.isLocked = true
        vm.refresh()

        assertTrue(vm.uiState.value.isLocked)
    }

    private companion object {
        val INSTAGRAM = AppEntry("com.instagram.android", "Instagram")
        val TIKTOK = AppEntry("com.zhiliaoapp.musically", "TikTok")
        val LAUNCHER = AppEntry("com.google.android.apps.nexuslauncher", "Pixel Launcher")
        val SELF = AppEntry("dev.focuslock", "FocusLock")
        val PLAY_STORE = AppEntry("com.android.vending", "Play Store")
    }
}
