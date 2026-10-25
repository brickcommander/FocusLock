package dev.focuslock.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.focuslock.BuildConfig
import dev.focuslock.pin.PinManager
import dev.focuslock.ui.theme.Coral400
import dev.focuslock.ui.theme.Coral500
import dev.focuslock.ui.theme.Teal400
import dev.focuslock.ui.theme.Teal500
import dev.focuslock.ui.theme.Teal800
import dev.focuslock.ui.theme.Teal900
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

private val cardShape = RoundedCornerShape(20.dp)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenAccessibilitySettings: () -> Unit,
    onActivateDeviceAdmin: () -> Unit,
    onRequestUninstall: () -> Unit,
    onOpenBlockList: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── App header ──
            Text(
                text = "FocusLock",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Stay focused. Stay free.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Protection-off warning ──
            if (state.isProtectionOff) {
                ProtectionOffBanner(
                    lastAppliedMs = state.lastAppliedMs,
                    onFix = if (!state.setup.blockerServiceEnabled) onOpenAccessibilitySettings else onActivateDeviceAdmin,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Status hero ──
            StatusHero(isLocked = state.isLocked, lastAppliedMs = state.lastAppliedMs)

            Spacer(modifier = Modifier.height(20.dp))

            // ── Lock / Unlock buttons ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { viewModel.lock() },
                    enabled = state.setup.isComplete && !state.isLocked,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Coral500,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("🔒  Lock", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    onClick = { viewModel.openPinDialog(PinDialogMode.UNLOCK) },
                    enabled = state.isLocked,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal500,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("🔓  Unlock", style = MaterialTheme.typography.titleMedium)
                }
            }

            // ── Break section ──
            val breakUntil = state.breakUntilMs
            if (breakUntil != null) {
                Spacer(modifier = Modifier.height(16.dp))
                BreakCard(breakUntil = breakUntil, onEndBreak = viewModel::endBreak)
                LaunchedEffect(breakUntil) {
                    delay((breakUntil - System.currentTimeMillis()).coerceAtLeast(0L))
                    viewModel.refresh()
                }
            } else if (state.isLocked) {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = viewModel::openBreakDialog,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("☕  Take a break")
                }
            }

            // ── Info message ──
            state.infoMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Setup checklist ──
            if (!state.setup.isComplete) {
                SetupChecklist(
                    status = state.setup,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onActivateDeviceAdmin = onActivateDeviceAdmin,
                    onSetCode = { viewModel.openPinDialog(PinDialogMode.SET) },
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Quick actions ──
            SectionCard(title = "Quick Actions") {
                ActionRow(
                    emoji = "📋",
                    label = if (state.isLocked) "View block list" else "Edit block list",
                    onClick = onOpenBlockList,
                )
                if (state.isPinSet) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionRow(
                        emoji = "🔑",
                        label = "Change code",
                        onClick = { viewModel.openPinDialog(PinDialogMode.CHANGE) },
                    )
                }
                if (state.isPinSet) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionRow(
                        emoji = "🗑️",
                        label = "Remove FocusLock completely",
                        onClick = viewModel::requestRemoveCompletely,
                        destructive = true,
                    )
                }
            }

            // ── DEV button ──
            if (BuildConfig.DEBUG && state.isLocked) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.devForceUnlock() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Coral500,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("DEV: Force unlock")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Dialogs ──
    when (state.pinDialog) {
        PinDialogMode.SET -> PinDialog(
            title = "Set unlock code",
            description = "Friend: enter a code (${PinManager.MIN_LENGTH}–${PinManager.MAX_LENGTH} characters). Don't share it.",
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

// ─── Components ───

@Composable
private fun StatusHero(isLocked: Boolean, lastAppliedMs: Long) {
    val bgColor by animateColorAsState(
        targetValue = if (isLocked) Coral500.copy(alpha = 0.12f) else Teal500.copy(alpha = 0.12f),
        animationSpec = tween(500),
        label = "statusBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isLocked) Coral400.copy(alpha = 0.4f) else Teal400.copy(alpha = 0.4f),
        animationSpec = tween(500),
        label = "statusBorder",
    )
    val accentColor by animateColorAsState(
        targetValue = if (isLocked) Coral400 else Teal400,
        animationSpec = tween(500),
        label = "statusAccent",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(bgColor)
            .border(1.5.dp, borderColor, cardShape)
            .padding(vertical = 28.dp, horizontal = 20.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
        ) {
            Text(
                text = if (isLocked) "🔒" else "🔓",
                fontSize = 36.sp,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isLocked) "LOCKED" else "UNLOCKED",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 3.sp,
        )
        if (isLocked && lastAppliedMs > 0L) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Since ${timeFormatter.format(Instant.ofEpochMilli(lastAppliedMs))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!isLocked) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your apps are accessible",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BreakCard(breakUntil: Long, onEndBreak: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Teal900, Teal800),
                )
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("☕", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "On a break until ${timeFormatter.format(Instant.ofEpochMilli(breakUntil))}",
            style = MaterialTheme.typography.titleMedium,
            color = Teal400,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onEndBreak,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal400),
        ) {
            Text("End break now")
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(20.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ActionRow(emoji: String, label: String, onClick: () -> Unit, destructive: Boolean = false) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = color)
        }
    }
}

@Composable
private fun ProtectionOffBanner(lastAppliedMs: Long, onFix: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Coral500, Coral400),
                )
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⚠️", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Protection is OFF",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Text(
            text = "FocusLock is locked, but can't block anything right now.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
        if (lastAppliedMs > 0L) {
            Text(
                text = "Last locked: ${timeFormatter.format(Instant.ofEpochMilli(lastAppliedMs))}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onFix,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Coral500,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Fix it", fontWeight = FontWeight.SemiBold)
        }
    }
}
