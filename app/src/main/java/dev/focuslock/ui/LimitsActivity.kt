package dev.focuslock.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.focuslock.AppContainer
import dev.focuslock.FocusApp
import dev.focuslock.ui.theme.FocusLockTheme

class LimitsActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as FocusApp).container

    private val viewModel: LimitsViewModel by viewModels {
        viewModelFactory {
            initializer {
                LimitsViewModel(
                    dailyLimits = container.dailyLimits,
                    editor = container.limitsEditor,
                    appSource = container.installedApps,
                    isPinSet = container.pinManager::isSet,
                    isInFocus = { container.lockManager.activeFocusSession() != null },
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusLockTheme {
                LimitsScreen(viewModel, onBack = ::finish)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
