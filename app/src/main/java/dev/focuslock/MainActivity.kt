package dev.focuslock

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.focuslock.blocker.FocusAccessibilityService
import dev.focuslock.ui.MainScreen
import dev.focuslock.ui.MainViewModel
import dev.focuslock.ui.theme.FocusLockTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as FocusApp).container

    private val viewModel: MainViewModel by viewModels {
        viewModelFactory {
            initializer {
                MainViewModel(
                    lockManager = container.lockManager,
                    lockStateStore = container.lockStateStore,
                    pinManager = container.pinManager,
                    isBlockerServiceEnabled = ::isBlockerServiceEnabled,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FocusLockTheme {
                MainScreen(
                    viewModel = viewModel,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun isBlockerServiceEnabled(): Boolean {
        val ours = ComponentName(this, FocusAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled.orEmpty().split(':').any { ComponentName.unflattenFromString(it) == ours }
    }
}
