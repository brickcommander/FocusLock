package dev.focuslock

import android.content.Context
import dev.focuslock.apps.InstalledApps
import dev.focuslock.blocker.BlockListEditor
import dev.focuslock.blocker.BlockRulesStore
import dev.focuslock.limits.DailyLimits
import dev.focuslock.limits.LimitsEditor
import dev.focuslock.limits.LimitsStore
import dev.focuslock.limits.UsageStore
import dev.focuslock.lock.LockManager
import dev.focuslock.lock.LockStateStore
import dev.focuslock.pin.PinHasher
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.PinStore
import dev.focuslock.pin.SystemClock
import dev.focuslock.setup.AndroidSetupChecker

class AppContainer(context: Context) {
    val lockStateStore: LockStateStore = LockStateStore(context)
    val blockRulesStore: BlockRulesStore = BlockRulesStore(context)
    val installedApps: InstalledApps = InstalledApps(context)
    val blockListEditor: BlockListEditor =
        BlockListEditor(blockRulesStore, lockStateStore, installedApps::protectedPackages)
    val pinManager: PinManager = PinManager(PinStore(context), PinHasher(), SystemClock)
    val setupChecker: AndroidSetupChecker = AndroidSetupChecker(context)
    val limitsStore: LimitsStore = LimitsStore(context)
    val lockManager: LockManager =
        LockManager(lockStateStore, pinManager, setupChecker, SystemClock, clearDailyLimits = limitsStore::clear)
    val dailyLimits: DailyLimits = DailyLimits(limitsStore, UsageStore(context), SystemClock)
    val limitsEditor: LimitsEditor = LimitsEditor(
        store = limitsStore,
        pinManager = pinManager,
        isInFocus = { lockManager.activeFocusSession() != null },
        protectedPackages = installedApps::protectedPackages,
    )
}
