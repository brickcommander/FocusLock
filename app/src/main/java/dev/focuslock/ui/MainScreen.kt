package dev.focuslock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.focuslock.BuildConfig

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "FocusLock",
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (state.isDeviceOwner) "Device Owner: YES" else "Device Owner: NO",
                style = MaterialTheme.typography.headlineSmall,
                color = if (state.isDeviceOwner) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )

            if (!state.isDeviceOwner) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Run this command to set Device Owner:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "adb shell dpm set-device-owner dev.focuslock/.admin.FocusAdminReceiver",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (BuildConfig.DEBUG && state.isDeviceOwner) {
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { viewModel.clearDeviceOwner() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                    ),
                ) {
                    Text("DEV: Remove Device Owner")
                }
            }
        }
    }
}
