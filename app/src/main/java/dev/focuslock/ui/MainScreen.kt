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

    // A minute-level clock for the countdown; the screen refreshes itself when the session ends.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val endsAt = focusUntil ?: breakUntil
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
) {
    val status = LocalStatusColors.current
    val focusUntil = state.focusUntilMs
    val breakUntil = state.breakUntilMs

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
            Text("Can't be ended early.", style = MaterialTheme.typography.bodySmall, color = accent)
        }

        Spacer(modifier = Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            when {
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
        if (!state.isLocked && !state.setup.isComplete) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Finish the setup below to lock.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
