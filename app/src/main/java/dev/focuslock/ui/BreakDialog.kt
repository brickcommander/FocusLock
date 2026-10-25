package dev.focuslock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.focuslock.lock.LockManager

@Composable
fun BreakDialog(
    message: PinMessage?,
    isBusy: Boolean,
    onSubmit: (pin: String, hours: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val range = LockManager.BREAK_HOURS
    var hours by remember { mutableIntStateOf(DEFAULT_HOURS) }
    var pin by remember { mutableStateOf("") }

    AppDialog(
        title = "Take a break",
        onDismiss = onDismiss,
        confirmLabel = "Start break",
        onConfirm = { onSubmit(pin, hours) },
        confirmEnabled = !isBusy,
        dismissEnabled = !isBusy,
    ) {
        DialogBody("Unblocks all apps and websites, and pauses daily limits. Blocking returns on its own. Your friend enters the code.")
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            StepButton("\u2212", onClick = { hours-- }, enabled = !isBusy && hours > range.first, description = "One hour less")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$hours", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (hours == 1) "HOUR" else "HOURS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StepButton("+", onClick = { hours++ }, enabled = !isBusy && hours < range.last, description = "One hour more")
        }
        Spacer(modifier = Modifier.height(20.dp))
        CodeField(value = pin, onValueChange = { pin = it }, label = "Code", enabled = !isBusy)
        DialogStatus(message, isBusy)
    }
}

private const val DEFAULT_HOURS = 2
