package dev.focuslock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import dev.focuslock.lock.LockReport

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "FocusLock",
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Device Owner status
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
                Spacer(modifier = Modifier.height(8.dp))
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

            Spacer(modifier = Modifier.height(16.dp))

            // Lock status
            Text(
                text = if (state.isLocked) "Status: LOCKED" else "Status: UNLOCKED",
                style = MaterialTheme.typography.titleLarge,
                color = if (state.isLocked) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Lock / Unlock buttons
            Row {
                Button(
                    onClick = { viewModel.lock() },
                    enabled = state.isDeviceOwner && !state.isLocked,
                ) {
                    Text("Lock")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { viewModel.unlock() },
                    enabled = state.isDeviceOwner && state.isLocked,
                ) {
                    Text("Unlock")
                }
            }

            // Last report
            state.lastReport?.let { report ->
                Spacer(modifier = Modifier.height(24.dp))
                ReportSection(report)
            }

            // DEV: Remove Device Owner
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

@Composable
private fun ReportSection(report: LockReport) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        if (report.applied.isNotEmpty()) {
            Text(
                text = "Applied (${report.applied.size}):",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            report.applied.forEach { pkg ->
                Text(
                    text = "  • $pkg",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (report.skippedNotInstalled.isNotEmpty()) {
            Text(
                text = "Skipped — not installed (${report.skippedNotInstalled.size}):",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            report.skippedNotInstalled.forEach { pkg ->
                Text(
                    text = "  • $pkg",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (report.failed.isNotEmpty()) {
            Text(
                text = "Failed (${report.failed.size}):",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
            )
            report.failed.forEach { pkg ->
                Text(
                    text = "  • $pkg",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
