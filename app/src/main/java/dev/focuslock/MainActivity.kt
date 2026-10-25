package dev.focuslock

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
                    onActivateDeviceAdmin = {
                        startActivity(
                            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, container.setupChecker.adminComponent)
                                .putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    "Stops FocusLock from being uninstalled while locked.",
                                )
                        )
                    },
                    onRequestUninstall = {
                        startActivity(Intent(Intent.ACTION_DELETE, "package:$packageName".toUri()))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
