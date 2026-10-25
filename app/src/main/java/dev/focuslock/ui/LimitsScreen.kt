package dev.focuslock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.focuslock.limits.DailyLimit
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun LimitsScreen(viewModel: LimitsViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text(
                    text = "Daily limits",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Give a blocked app a daily allowance. Once it's used up, the app stays blocked until midnight.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!state.canEdit) {
                Text(
                    text = "Unlock to change limits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            state.message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }

            Text(
                text = "BLOCKED APPS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (state.rows.isEmpty()) {
                    Text(
                        text = "No apps on the block list yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
                state.rows.forEachIndexed { index, row ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    LimitRowItem(row = row, canEdit = state.canEdit, onClick = { viewModel.openEditor(row.packageName) })
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    state.draft?.let { draft ->
        LimitEditorDialog(
            draft = draft,
            onSetDay = viewModel::setDay,
            onSetEveryDay = viewModel::setEveryDay,
            onSave = viewModel::saveDraft,
            onDismiss = viewModel::dismissEditor,
        )
    }
}

@Composable
private fun LimitRowItem(row: LimitRow, canEdit: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canEdit, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(row.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = rowSummary(row),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (canEdit) {
            Text(
                text = "Edit",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun rowSummary(row: LimitRow): String = when {
    row.limit.isEmpty -> "No allowance \u00B7 always blocked"
    row.allowedTodayMs <= 0L -> "No allowance today"
    else -> {
        val left = (row.allowedTodayMs - row.usedTodayMs).coerceAtLeast(0L)
        if (left == 0L) "Used up today \u00B7 resets at midnight"
        else "${formatMs(left)} left of ${formatMs(row.allowedTodayMs)} today"
    }
}

@Composable
private fun LimitEditorDialog(
    draft: LimitDraft,
    onSetDay: (DayOfWeek, Int) -> Unit,
    onSetEveryDay: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val limit = draft.limit
    val uniform = limit.minutes.distinct().size == 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(draft.label, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Time allowed each day. \"Blocked\" means no allowance that day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                StepperRow(
                    label = "Every day",
                    minutes = if (uniform) limit.minutes.first() else null,
                    onChange = onSetEveryDay,
                    emphasized = true,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                DayOfWeek.entries.forEach { day ->
                    StepperRow(
                        label = day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        minutes = limit.minutesOn(day),
                        onChange = { onSetDay(day, it) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(16.dp),
    )
}

/** @param minutes null when the days differ (the "every day" row then starts from 0). */
@Composable
private fun StepperRow(label: String, minutes: Int?, onChange: (Int) -> Unit, emphasized: Boolean = false) {
    val current = minutes ?: 0
    val step = DailyLimit.STEP_MINUTES
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = { onChange(current - step) },
            enabled = current > 0,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.width(44.dp),
            contentPadding = PaddingValues(0.dp),
        ) { Text("\u2212") }
        Text(
            text = minutes?.let(::formatMinutes) ?: "Mixed",
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp),
        )
        OutlinedButton(
            onClick = { onChange(current + step) },
            enabled = current < DailyLimit.MAX_MINUTES,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.width(44.dp),
            contentPadding = PaddingValues(0.dp),
        ) { Text("+") }
    }
}

internal fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "Blocked"
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
