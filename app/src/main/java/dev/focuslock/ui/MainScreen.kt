package dev.focuslock.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.focuslock.BuildConfig
import dev.focuslock.lock.EmergencyState
import dev.focuslock.pin.PinManager
import dev.focuslock.ui.theme.LocalStatusColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

private fun clock(ms: Long): String = timeFormatter.format(Instant.ofEpochMilli(ms))

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
    val focusUntil = state.focusUntilMs
    val breakUntil = state.breakUntilMs

    val emergency = state.emergency

    // A minute-level clock for the countdown; the screen refreshes itself when the session ends.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val emergencyDeadline = when (emergency) {
        is EmergencyState.Countdown -> emergency.unlockAtMs
        is EmergencyState.TemporaryUnlock -> emergency.relockAtMs
        null -> null
    }
    val endsAt = focusUntil ?: breakUntil ?: emergencyDeadline
    if (endsAt != null) {
        LaunchedEffect(endsAt) {
            while (true) {
                now = System.currentTimeMillis()
                val left = endsAt - now
                if (left <= 0L) break
                delay(left.coerceAtMost(TICK_MS))
            }
            viewModel.refresh()
        }
    }

    AppScreen(title = "FocusLock") {
        if (state.isProtectionOff) {
            Banner(
                title = "Protection is off",
                body = "Locked, but nothing can be blocked right now.",
                tone = BannerTone.Danger,
                actionLabel = "Fix",
                onAction = if (!state.setup.blockerServiceEnabled) onOpenAccessibilitySettings else onActivateDeviceAdmin,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        StatusCard(
            state = state,
            now = now,
            onLock = viewModel::lock,
            onUnlock = { viewModel.openPinDialog(PinDialogMode.UNLOCK) },
            onEndBreak = viewModel::endBreak,
            onForgotCode = viewModel::openEmergencyConfirm,
            onCancelEmergency = viewModel::cancelEmergencyUnlock,
        )

        state.infoMessage?.let { InfoText(it) }

        if (!state.setup.isComplete) {
            SectionLabel("Setup")
            SetupChecklist(
                status = state.setup,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onActivateDeviceAdmin = onActivateDeviceAdmin,
                onSetCode = { viewModel.openPinDialog(PinDialogMode.SET) },
            )
        }

        val canBreak = state.isLocked && focusUntil == null && breakUntil == null
        val canFocus = focusUntil == null && state.setup.isComplete
        if (canBreak || canFocus) {
            SectionLabel("Sessions")
            GroupCard {
                if (canBreak) {
                    ListRow(
                        title = "Take a break",
                        subtitle = "Unblock everything for a while. Needs the code.",
                        onClick = viewModel::openBreakDialog,
                    )
                }
                if (canBreak && canFocus) RowDivider()
                if (canFocus) {
                    ListRow(
                        title = "Focus session",
                        subtitle = "Block everything for 1\u20136 hours. Can't be ended early.",
                        onClick = viewModel::openFocusDialog,
                    )
                }
            }
        }

        SectionLabel("Manage")
        GroupCard {
            ListRow(
                title = "Block list",
                subtitle = if (state.isLocked || focusUntil != null) "Apps and websites \u00B7 view only" else "Apps and websites",
                onClick = onOpenBlockList,
            )
            RowDivider()
            ListRow(
                title = "Daily limits",
                subtitle = "Time allowed per app each day, locked or not",
                onClick = onOpenLimits,
            )
            if (state.isPinSet) {
                RowDivider()
                ListRow(title = "Change code", onClick = { viewModel.openPinDialog(PinDialogMode.CHANGE) })
            }
        }

        if (state.isPinSet && focusUntil == null) {
            Spacer(modifier = Modifier.height(20.dp))
            GroupCard {
                ListRow(
                    title = "Remove FocusLock",
                    subtitle = "Turns off all protection and uninstalls",
                    onClick = viewModel::requestRemoveCompletely,
                    destructive = true,
                )
            }
        }

        if (BuildConfig.DEBUG && (state.isLocked || focusUntil != null)) {
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = viewModel::devForceUnlock,
                modifier = Modifier.padding(horizontal = ScreenPadding - 12.dp),
            ) {
                Text("DEV: Force unlock", color = LocalStatusColors.current.locked)
            }
        }
    }

    // ── Dialogs ──
    when (state.pinDialog) {
        PinDialogMode.SET -> PinDialog(
            title = "Set the code",
            description = "Hand your phone to your friend. They choose a code (${PinManager.MIN_LENGTH}\u2013${PinManager.MAX_LENGTH} characters) that only they know.",
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
            description = "Enter the code to remove all protection, clear daily limits and uninstall.",
            fieldLabels = listOf("Code"),
            message = state.pinMessage,
            isBusy = state.isBusy,
            confirmLabel = "Remove",
            onSubmit = { viewModel.submitRemoveCompletely(it[0]) },
            onDismiss = viewModel::dismissPinDialog,
            destructive = true,
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
        AppDialog(
            title = "Remove FocusLock?",
            onDismiss = viewModel::cancelRemoveCompletely,
            confirmLabel = "Continue",
            onConfirm = viewModel::confirmRemoveCompletely,
            destructive = true,
        ) {
            DialogBody("This unlocks, clears daily limits, turns off uninstall protection and uninstalls FocusLock.")
        }
    }

    if (state.showEmergencyConfirm) {
        EmergencyConfirmDialog(
            onConfirm = viewModel::confirmEmergencyUnlock,
            onDismiss = viewModel::dismissEmergencyConfirm,
        )
    }

    LaunchedEffect(state.uninstallRequested) {
        if (state.uninstallRequested) {
            onRequestUninstall()
            viewModel.onUninstallLaunched()
        }
    }
}

@Composable
private fun StatusCard(
    state: MainUiState,
    now: Long,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    onEndBreak: () -> Unit,
    onForgotCode: () -> Unit,
    onCancelEmergency: () -> Unit,
) {
    val status = LocalStatusColors.current
    val focusUntil = state.focusUntilMs
    val breakUntil = state.breakUntilMs
    val emergency = state.emergency

    data class Look(val label: String, val headline: String, val detail: String?, val accent: Color, val tint: Color)

    val look = when {
        focusUntil != null -> Look(
            "FOCUS SESSION", "Focusing", "${formatLeft(focusUntil - now)} left \u00B7 until ${clock(focusUntil)}",
            status.focus, status.focusTint,
        )
        breakUntil != null -> Look(
            "BREAK", "On a break", "${formatLeft(breakUntil - now)} left \u00B7 blocking resumes at ${clock(breakUntil)}",
            status.free, status.freeTint,
        )
        emergency is EmergencyState.TemporaryUnlock -> Look(
            "TEMPORARY UNLOCK", "Unlocked",
            "Locks again in ${formatLeft(emergency.relockAtMs - now)} \u00B7 at ${clock(emergency.relockAtMs)}",
            status.locked, status.lockedTint,
        )
        state.isLocked -> Look(
            "LOCKED", "Locked", state.lastAppliedMs.takeIf { it > 0L }?.let { "Since ${clock(it)}" },
            status.locked, status.lockedTint,
        )
        else -> Look("UNLOCKED", "Unlocked", "Daily limits still apply", status.free, status.freeTint)
    }
    val tint by animateColorAsState(look.tint, tween(500), label = "tint")
    val accent by animateColorAsState(look.accent, tween(500), label = "accent")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(tint)
            .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(accent)
            Spacer(modifier = Modifier.width(8.dp))
            Text(look.label, style = MaterialTheme.typography.labelMedium, color = accent)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(look.headline, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface)
        look.detail?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (focusUntil != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Can\u2019t be ended early.", style = MaterialTheme.typography.bodySmall, color = accent)
        }

        // Emergency countdown while locked
        if (emergency is EmergencyState.Countdown) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "Emergency unlock in ${formatLeft(emergency.unlockAtMs - now)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Unlocks at ${clock(emergency.unlockAtMs)} for 1 hour, then locks again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Temporary unlock warning
        if (emergency is EmergencyState.TemporaryUnlock) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Contact the person who set your code, or change your block list now.",
                style = MaterialTheme.typography.bodySmall,
                color = accent,
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            when {
                emergency is EmergencyState.Countdown -> {
                    SecondaryButton("Cancel emergency", onClick = onCancelEmergency, modifier = Modifier.weight(1f))
                }
                emergency is EmergencyState.TemporaryUnlock -> {
                    PrimaryButton("Lock now", onClick = onLock, enabled = state.setup.isComplete, modifier = Modifier.weight(1f))
                }
                breakUntil != null -> {
                    PrimaryButton("End break now", onClick = onEndBreak, modifier = Modifier.weight(1f))
                }
                state.isLocked -> {
                    SecondaryButton(
                        "Unlock",
                        onClick = onUnlock,
                        enabled = focusUntil == null,
                        modifier = Modifier.weight(1f),
                    )
                }
                else -> {
                    PrimaryButton("Lock", onClick = onLock, enabled = state.setup.isComplete, modifier = Modifier.weight(1f))
                }
            }
        }

        // "Forgot code?" link — shown when locked, no focus, no emergency pending
        if (state.isLocked && focusUntil == null && emergency == null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onForgotCode) {
                    Text(
                        "Forgot code?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!state.isLocked && emergency == null && !state.setup.isComplete) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Finish the setup below to lock.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmergencyConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        title = "Emergency unlock",
        onDismiss = onDismiss,
        confirmLabel = "Start countdown",
        onConfirm = onConfirm,
    ) {
        DialogBody("Your phone will unlock after a 12-hour wait.")
        Spacer(modifier = Modifier.height(12.dp))
        DialogBody("You\u2019ll have 1 hour to use your phone freely. After that, FocusLock locks again automatically.")
        Spacer(modifier = Modifier.height(12.dp))
        DialogBody("This prevents impulse unlocking \u2014 you can\u2019t wait 12 hours on impulse.")
    }
}

private fun formatLeft(ms: Long): String {
    val minutes = ((ms.coerceAtLeast(0L) + 59_999L) / 60_000L)
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "${m}m"
        m == 0L -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

private const val TICK_MS = 30_000L
