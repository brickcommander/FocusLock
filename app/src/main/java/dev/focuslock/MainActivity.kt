package dev.focuslock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.focuslock.policy.BlockList
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
                    policyController = container.policyController,
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
                    onLogBrowserSchema = { container.schemaReader.logSchema(BlockList.CHROME) },
                )
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.reconcile()
    }
}
