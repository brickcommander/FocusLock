package dev.focuslock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.focuslock.BuildConfig
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenAccessibilitySettings: () -> Unit,
    onActivateDeviceAdmin: () -> Unit,
    onRequestUninstall: () -> Unit,
    onOpenBlockList: () -> Unit,
) {
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

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isProtectionOff) {
                ProtectionOffBanner(
                    lastAppliedMs = state.lastAppliedMs,
                    onFix = if (!state.setup.blockerServiceEnabled) onOpenAccessibilitySettings else onActivateDeviceAdmin,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            SetupChecklist(
                status = state.setup,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onActivateDeviceAdmin = onActivateDeviceAdmin,
                onSetCode = { viewModel.openPinDialog(PinDialogMode.SET) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (state.isLocked) "Status: LOCKED" else "Status: UNLOCKED",
                style = MaterialTheme.typography.titleLarge,
                color = if (state.isLocked) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )

            if (state.isLocked && state.lastAppliedMs > 0L) {
                Text(
                    text = lastLockedText(state.lastAppliedMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                Button(
                    onClick = { viewModel.lock() },
                    enabled = state.setup.isComplete && !state.isLocked,
                ) {
                    Text("Lock")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { viewModel.openPinDialog(PinDialogMode.UNLOCK) },
                    enabled = state.isLocked,
                ) {
                    Text("Unlock")
                }
            }

            val breakUntil = state.breakUntilMs
            if (breakUntil != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "☕ On a break until ${timeFormatter.format(Instant.ofEpochMilli(breakUntil))}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(onClick = viewModel::endBreak) {
                    Text("End break now")
                }
                LaunchedEffect(breakUntil) {
                    delay((breakUntil - System.currentTimeMillis()).coerceAtLeast(0L))
                    viewModel.refresh()
                }
            } else if (state.isLocked) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = viewModel::openBreakDialog) {
                    Text("Take a break")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenBlockList) {
                Text(if (state.isLocked) "View block list" else "Edit block list")
            }

            if (state.isPinSet) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.openPinDialog(PinDialogMode.CHANGE) }) {
                    Text("Change code")
                }
            }

            state.infoMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            if (state.isPinSet) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = viewModel::requestRemoveCompletely) {
                    Text("Remove FocusLock completely")
                }
            }

            if (BuildConfig.DEBUG && state.isLocked) {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.devForceUnlock() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                ) {
                    Text("DEV: Force unlock")
                }
            }
        }
    }

    when (state.pinDialog) {
        PinDialogMode.SET -> PinDialog(
            title = "Set unlock code",
            description = "Friend: enter a 6–12 digit code. Don't share it.",
            fieldLabels = listOf("New code", "Confirm code"),
            message = state.pinMessage,
            isBusy = state.isBusy,
            confirmLabel = "Save",
            onSubmit = { viewModel.submitSetPin(it[0], it[1]) },
            onDismiss = viewModel::dismissPinDialog,
        )
        PinDialogMode.UNLOCK -> PinDialog(
            title = "Enter code to unlock",
            fieldLabels = listOf("Code"),
            message = state.pinMessage,
            isBusy = state.isBusy,
            confirmLabel = "Unlock",
            onSubmit = { viewModel.submitUnlock(it[0]) },
            onDismiss = viewModel::dismissPinDialog,
        )
        PinDialogMode.CHANGE -> PinDialog(
            title = "Change code",
            fieldLabels = listOf("Current code", "New code", "Confirm new code"),
            message = state.pinMessage,
            isBusy = state.isBusy,
            confirmLabel = "Change",
            onSubmit = { viewModel.submitChangePin(it[0], it[1], it[2]) },
            onDismiss = viewModel::dismissPinDialog,
        )
        PinDialogMode.REMOVE -> PinDialog(
            title = "Enter code to remove FocusLock",
            fieldLabels = listOf("Code"),
            message = state.pinMessage,
            isBusy = state.isBusy,
            confirmLabel = "Remove",
            onSubmit = { viewModel.submitRemoveCompletely(it[0]) },
            onDismiss = viewModel::dismissPinDialog,
        )
        null -> Unit
    }

    if (state.showBreakDialog) {
        BreakDialog(
            message = state.pinMessage,
            isBusy = state.isBusy,
            onSubmit = viewModel::submitBreak,
            onDismiss = viewModel::dismissBreakDialog,
        )
    }

    if (state.showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::cancelRemoveCompletely,
            title = { Text("Remove FocusLock completely?") },
            text = {
                Text("This removes all protection: it unlocks, turns off uninstall protection, and then asks Android to uninstall FocusLock.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRemoveCompletely) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRemoveCompletely) { Text("Cancel") }
            },
        )
    }

    LaunchedEffect(state.uninstallRequested) {
        if (state.uninstallRequested) {
            onRequestUninstall()
            viewModel.onUninstallLaunched()
        }
    }
}

private fun lastLockedText(epochMs: Long): String =
    "Last locked: ${timeFormatter.format(Instant.ofEpochMilli(epochMs))}"

@Composable
private fun ProtectionOffBanner(lastAppliedMs: Long, onFix: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Protection is OFF",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onError,
        )
        Text(
            text = "FocusLock is locked, but it can't block anything right now.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onError,
        )
        if (lastAppliedMs > 0L) {
            Text(
                text = lastLockedText(lastAppliedMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onError,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onFix,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onError,
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text("Fix it")
        }
    }
}
