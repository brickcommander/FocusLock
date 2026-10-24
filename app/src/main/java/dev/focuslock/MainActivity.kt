package dev.focuslock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.focuslock.ui.MainScreen
import dev.focuslock.ui.MainViewModel
import dev.focuslock.ui.theme.FocusLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as FocusApp).container
        val viewModel = MainViewModel(container.policyController)

        setContent {
            FocusLockTheme {
                MainScreen(viewModel)
            }
        }
    }
}
