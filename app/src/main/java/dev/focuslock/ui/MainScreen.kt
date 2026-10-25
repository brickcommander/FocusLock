package dev.focuslock.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.focuslock.BuildConfig
import dev.focuslock.pin.PinManager
import dev.focuslock.ui.theme.Neutral700
import dev.focuslock.ui.theme.StatusLocked
import dev.focuslock.ui.theme.StatusUnlocked
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
    onOpenLimits: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Header ──
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text(
                    text = "FocusLock",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // ── Protection-off warning ──
            if (state.isProtectionOff) {
                WarningBanner(
                    lastAppliedMs = state.lastAppliedMs,
                    onFix = if (!state.setup.blockerServiceEnabled) onOpenAccessibilitySettings else onActivateDeviceAdmin,
                )
            }

            // ── Status card ──
            StatusCard(isLocked = state.isLocked, lastAppliedMs = state.lastAppliedMs, focusUntilMs = state.focusUntilMs)

            // ── Lock / Unlock ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth(),
            ) {
                Button(
                    onClick = { viewModel.lock() },
                    enabled = state.setup.isComplete && !state.isLocked,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                    ),
                ) {
                    Text("Lock", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                    onClick = { viewModel.openPinDialog(PinDialogMode.UNLOCK) },
                    enabled = state.isLocked && !state.isInFocus,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Unlock", style = MaterialTheme.typography.titleMedium)
                }
            }

            // ── Focus session / break ──
            val focusUntil = state.focusUntilMs
            val breakUntil = state.breakUntilMs
            if (focusUntil != null) {
                LaunchedEffect(focusUntil) {
                    delay((focusUntil - System.currentTimeMillis()).coerceAtLeast(0L))
                    viewModel.refresh()
                }
            } else if (breakUntil != null) {
                BreakBanner(breakUntil = breakUntil, onEnd = viewModel::endBreak)
                LaunchedEffect(breakUntil) {
                    delay((breakUntil - System.currentTimeMillis()).coerceAtLeast(0L))
                    viewModel.refresh()
                }
            } else if (state.isLocked) {
                MenuItem(
                    label = "Take a break",
                    secondary = "Temporarily unblock everything",
                    onClick = viewModel::openBreakDialog,
                )
            }
            if (focusUntil == null && state.setup.isComplete) {
                MenuItem(
                    label = "Start focus session",
                    secondary = "Block everything for 1\u20136 hours. Can't be ended early.",
                    onClick = viewModel::openFocusDialog,
                )
            }

            // ── Info message ──
            state.infoMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }

            // ── Setup ──
            if (!state.setup.isComplete) {
                SectionLabel("Setup")
                SetupChecklist(
                    status = state.setup,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onActivateDeviceAdmin = onActivateDeviceAdmin,
                    onSetCode = { viewModel.openPinDialog(PinDialogMode.SET) },
                )
            }

            // ── Actions ──
            SectionLabel("Settings")

            Surface {
                MenuItem(
                    label = if (state.isLocked) "View block list" else "Edit block list",
                    secondary = "Apps and websites",
                    onClick = onOpenBlockList,
                    showDivider = true,
                )
                MenuItem(
                    label = "Daily limits",
                    secondary = "Daily time allowance for blocked apps",
                    onClick = onOpenLimits,
                    showDivider = state.isPinSet,
                )
                if (state.isPinSet) {
                    MenuItem(
                        label = "Change code",
                        onClick = { viewModel.openPinDialog(PinDialogMode.CHANGE) },
                    )
                }
            }

            if (state.isPinSet && !state.isInFocus) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface {
                    MenuItem(
                        label = "Remove FocusLock completely",
                        onClick = viewModel::requestRemoveCompletely,
                        destructive = true,
                    )
                }
            }

            // ── DEV ──
            if (BuildConfig.DEBUG && state.isLocked) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.devForceUnlock() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusLocked,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    Text("DEV: Force unlock")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ── Dialogs ──
    when (state.pinDialog) {
        PinDialogMode.SET -> PinDialog(
            title = "Set unlock code",
            description = "Hand your phone to your friend. They enter a code (${PinManager.MIN_LENGTH}–${PinManager.MAX_LENGTH} characters) that only they know.",
            fieldLabels = listOf("New code", "Confirm code"),
            message = state.pinMessage,
            isBusy = state.isBusy,
            confirmLabel = "Save",
            onSubmit = { viewModel.submitSetPin(it[0], it[1]) },
            onDismiss = viewModel::dismissPinDialog,
        )
        PinDialogMode.UNLOCK -> PinDialog(
            title = "Unlock",
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
            title = "Remove FocusLock",
            description = "Enter the code to remove all protection and uninstall.",
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

    if (state.showFocusDialog) {
        FocusSessionDialog(onStart = viewModel::startFocusSession, onDismiss = viewModel::dismissFocusDialog)
    }

    if (state.showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::cancelRemoveCompletely,
            title = { Text("Remove FocusLock?") },
            text = {
                Text("This will unlock, turn off uninstall protection, and uninstall FocusLock from this device.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRemoveCompletely) {
                    Text("Continue", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRemoveCompletely) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp),
        )
    }

    LaunchedEffect(state.uninstallRequested) {
        if (state.uninstallRequested) {
            onRequestUninstall()
            viewModel.onUninstallLaunched()
        }
    }
}

// ─── Shared components ───

@Composable
private fun StatusCard(isLocked: Boolean, lastAppliedMs: Long, focusUntilMs: Long?) {
    val blocking = isLocked || focusUntilMs != null
    val dotColor by animateColorAsState(
        targetValue = if (blocking) StatusLocked else StatusUnlocked,
        animationSpec = tween(400),
        label = "dot",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = when {
                    focusUntilMs != null -> "Focus session"
                    isLocked -> "Locked"
                    else -> "Unlocked"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (focusUntilMs != null) {
                Text(
                    text = "Until ${timeFormatter.format(Instant.ofEpochMilli(focusUntilMs))} \u00B7 can't be ended early",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (isLocked && lastAppliedMs > 0L) {
                Text(
                    text = "Since ${timeFormatter.format(Instant.ofEpochMilli(lastAppliedMs))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BreakBanner(breakUntil: Long, onEnd: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Break active",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "Until ${timeFormatter.format(Instant.ofEpochMilli(breakUntil))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
        TextButton(onClick = onEnd) {
            Text("End", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun Surface(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        content()
    }
}

@Composable
private fun MenuItem(
    label: String,
    secondary: String? = null,
    onClick: () -> Unit,
    destructive: Boolean = false,
    showDivider: Boolean = false,
) {
    val textColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
        )
        secondary?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (showDivider) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun WarningBanner(lastAppliedMs: Long, onFix: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Protection is off",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = "Locked, but unable to block anything.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
            )
        }
        TextButton(onClick = onFix) {
            Text("Fix", color = MaterialTheme.colorScheme.error)
        }
    }
}
