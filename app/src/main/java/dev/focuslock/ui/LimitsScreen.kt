package dev.focuslock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.focuslock.limits.DailyLimit
import dev.focuslock.ui.theme.LocalStatusColors
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun LimitsScreen(viewModel: LimitsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    AppScreen(
        title = "Daily limits",
        subtitle = "Give an app a daily time budget. When it runs out, the app is blocked until midnight, locked or not.",
        onBack = onBack,
    ) {
        if (state.isInFocus) {
            Banner(title = "Focus session running", body = "Apps with a limit are fully blocked until it ends.", tone = BannerTone.Focus)
        }
        state.message?.let { InfoText(it) }

        SectionLabel("Apps \u00B7 ${state.rows.size}")
        GroupCard {
            if (state.rows.isEmpty()) EmptyRow("No limits yet")
            state.rows.forEachIndexed { index, row ->
                if (index > 0) RowDivider()
                LimitRowItem(row = row, enabled = !state.isInFocus, onClick = { viewModel.openEditor(row.packageName) })
            }
            if (!state.isInFocus) {
                if (state.rows.isNotEmpty()) RowDivider()
                ListRow(title = "Add an app", onClick = viewModel::openPicker, leading = { Monogram("+") })
            }
        }
        Text(
            text = if (state.needsCode) "Adding, changing or removing a limit needs the code." else "Once a code is set, changes will need it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = ScreenPadding + 4.dp, vertical = 10.dp),
        )
    }

    if (state.showPicker) {
        AppPickerDialog(apps = state.pickerApps, onPick = viewModel::pickApp, onDismiss = viewModel::closePicker, title = "Limit an app")
    }

    state.draft?.let { draft ->
        LimitEditorDialog(
            draft = draft,
            needsCode = state.needsCode,
            message = state.pinMessage,
            isBusy = state.isBusy,
            onSetDay = viewModel::setDay,
            onSetEveryDay = viewModel::setEveryDay,
            onSave = viewModel::save,
            onRemove = viewModel::remove,
            onDismiss = viewModel::dismissEditor,
        )
    }
}

@Composable
private fun LimitRowItem(row: LimitRow, enabled: Boolean, onClick: () -> Unit) {
    val status = LocalStatusColors.current
    val usedUp = row.allowedTodayMs > 0L && row.leftTodayMs == 0L
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Monogram(row.label)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(row.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                rowSummary(row),
                style = MaterialTheme.typography.bodySmall,
                color = if (usedUp || row.allowedTodayMs == 0L) status.locked else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (row.allowedTodayMs > 0L) {
                Spacer(modifier = Modifier.height(8.dp))
                val used = (row.usedTodayMs.toFloat() / row.allowedTodayMs).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(used)
                            .fillMaxHeight()
                            .background(if (usedUp) status.locked else MaterialTheme.colorScheme.onSurface),
                    )
                }
            }
        }
        if (enabled) {
            Spacer(modifier = Modifier.width(14.dp))
            Chevron(color = MaterialTheme.colorScheme.outline)
        }
    }
}

private fun rowSummary(row: LimitRow): String = when {
    row.allowedTodayMs <= 0L -> "No time today \u00B7 blocked all day"
    row.leftTodayMs == 0L -> "Used up \u00B7 resets at midnight"
    else -> "${formatMs(row.leftTodayMs)} left of ${formatMs(row.allowedTodayMs)} today"
}

@Composable
private fun LimitEditorDialog(
    draft: LimitDraft,
    needsCode: Boolean,
    message: PinMessage?,
    isBusy: Boolean,
    onSetDay: (DayOfWeek, Int) -> Unit,
    onSetEveryDay: (Int) -> Unit,
    onSave: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val limit = draft.limit
    val uniform = limit.minutes.distinct().size == 1
    var pin by remember { mutableStateOf("") }

    AppDialog(
        title = draft.label,
        onDismiss = onDismiss,
        confirmLabel = if (draft.isNew) "Add" else "Save",
        onConfirm = { onSave(pin) },
        confirmEnabled = !isBusy,
        dismissEnabled = !isBusy,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            DialogBody("Time allowed each day. \"None\" blocks the app all day.")
            Spacer(modifier = Modifier.height(16.dp))
            StepperRow(
                label = "Every day",
                minutes = if (uniform) limit.minutes.first() else null,
                onChange = onSetEveryDay,
                enabled = !isBusy,
                emphasized = true,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
            DayOfWeek.entries.forEach { day ->
                StepperRow(
                    label = day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    minutes = limit.minutesOn(day),
                    onChange = { onSetDay(day, it) },
                    enabled = !isBusy,
                )
            }
        }
        if (needsCode) {
            Spacer(modifier = Modifier.height(16.dp))
            CodeField(value = pin, onValueChange = { pin = it }, label = "Code", enabled = !isBusy)
        }
        DialogStatus(message, isBusy)
        if (!draft.isNew) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = { onRemove(pin) }, enabled = !isBusy) {
                Text("Remove limit", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** @param minutes null when the days differ (the "every day" row then starts from 0). */
@Composable
private fun StepperRow(label: String, minutes: Int?, onChange: (Int) -> Unit, enabled: Boolean, emphasized: Boolean = false) {
    val current = minutes ?: 0
    val step = DailyLimit.STEP_MINUTES
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        StepButton("\u2212", onClick = { onChange(current - step) }, enabled = enabled && current > 0, description = "Less time on $label")
        Text(
            text = minutes?.let(::formatMinutes) ?: "Mixed",
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            color = if (current == 0 && minutes != null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(min = 76.dp),
        )
        StepButton("+", onClick = { onChange(current + step) }, enabled = enabled && current < DailyLimit.MAX_MINUTES, description = "More time on $label")
    }
}

internal fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "None"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

internal fun formatMs(ms: Long): String {
    val totalMinutes = ms / 60_000L
    if (totalMinutes == 0L) return if (ms > 0L) "<1m" else "0m"
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0L -> "${m}m"
        m == 0L -> "${h}h"
        else -> "${h}h ${m}m"
    }
}
