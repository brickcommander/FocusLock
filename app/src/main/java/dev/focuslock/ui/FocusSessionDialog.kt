package dev.focuslock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.focuslock.lock.LockManager
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start focus session", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    "Everything on your block list stays blocked until the session ends, even if FocusLock is unlocked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    options.forEach { option ->
                        FilterChip(
                            selected = option == hours,
                            onClick = { hours = option },
                            label = { Text("${option}h") },
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Ends at $endsAt",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "It can't be ended early: not with the code, not by unlocking, not with a break.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onStart(hours) }) {
                Text("Start", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(16.dp),
    )
}
