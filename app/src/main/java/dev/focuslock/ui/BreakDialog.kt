package dev.focuslock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.focuslock.lock.LockManager
import dev.focuslock.pin.PinManager

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("☕ Take a break", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column {
                Text(
                    "Unblocks all apps and websites, then blocking comes back by itself.\nFriend: enter the code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { hours-- },
                        enabled = !isBusy && hours > range.first,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("−")
                    }
                    Text(
                        text = if (hours == 1) "1 hour" else "$hours hours",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    OutlinedButton(
                        onClick = { hours++ },
                        enabled = !isBusy && hours < range.last,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("+")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input -> pin = input.take(PinManager.MAX_LENGTH) },
                    label = { Text("Code") },
                    singleLine = true,
                    enabled = !isBusy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                message?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        it.toText(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (isBusy) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(pin, hours) }, enabled = !isBusy) {
                Text("Start break")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

private const val DEFAULT_HOURS = 2
