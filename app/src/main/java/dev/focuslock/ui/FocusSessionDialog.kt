package dev.focuslock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.focuslock.lock.LockManager
import dev.focuslock.ui.theme.LocalStatusColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val endFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun FocusSessionDialog(onStart: (hours: Int) -> Unit, onDismiss: () -> Unit) {
    val options = LockManager.FOCUS_HOURS
    var hours by remember { mutableIntStateOf(options.first()) }
    val endsAt = endFormatter.format(Instant.ofEpochMilli(System.currentTimeMillis() + hours * 3_600_000L))
    val status = LocalStatusColors.current

    AppDialog(
        title = "Focus session",
        onDismiss = onDismiss,
        confirmLabel = "Start",
        onConfirm = { onStart(hours) },
        destructive = true,
    ) {
        DialogBody("Blocks your block list and every app with a daily limit, even if FocusLock is unlocked.")
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                val selected = option == hours
                val shape = RoundedCornerShape(14.dp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(shape)
                        .background(if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, shape)
                        .clickable(role = Role.RadioButton) { hours = option }
                        .semantics { this.selected = selected },
                ) {
                    Text(
                        "${option}h",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text("Ends at $endsAt", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            "Can't be ended early: not with the code, not by unlocking, not with a break.",
            style = MaterialTheme.typography.bodySmall,
            color = status.locked,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(status.lockedTint)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
