package dev.focuslock.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class FocusAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Turning this off removes FocusLock's protection. Ask your friend for the code instead."
}
