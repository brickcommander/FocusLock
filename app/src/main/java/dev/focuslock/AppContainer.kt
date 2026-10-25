package dev.focuslock

import android.content.Context
import dev.focuslock.lock.LockManager
import dev.focuslock.lock.LockStateStore
import dev.focuslock.pin.PinHasher
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.PinStore
import dev.focuslock.pin.SystemClock
import dev.focuslock.setup.AndroidSetupChecker

class AppContainer(context: Context) {
    val lockStateStore: LockStateStore = LockStateStore(context)
    val pinManager: PinManager = PinManager(PinStore(context), PinHasher(), SystemClock)
    val setupChecker: AndroidSetupChecker = AndroidSetupChecker(context)
    val lockManager: LockManager = LockManager(lockStateStore, pinManager, setupChecker, SystemClock)
}
