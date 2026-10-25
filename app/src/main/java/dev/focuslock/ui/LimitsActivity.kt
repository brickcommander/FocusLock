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
                    rulesStore = container.blockRulesStore,
                    appSource = container.installedApps,
                    isBlocking = container.lockManager::isBlocking,
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusLockTheme {
                LimitsScreen(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
