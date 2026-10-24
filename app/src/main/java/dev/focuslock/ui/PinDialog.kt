package dev.focuslock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.focuslock.pin.PinManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PinDialog(
    title: String,
    fieldLabels: List<String>,
    message: PinMessage?,
    isBusy: Boolean,
    confirmLabel: String,
    onSubmit: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    description: String? = null,
) {
    val values = remember(fieldLabels) { mutableStateListOf(*Array(fieldLabels.size) { "" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                description?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                fieldLabels.forEachIndexed { index, label ->
                    OutlinedTextField(
                        value = values[index],
                        onValueChange = { input ->
                            values[index] = input.filter { it in '0'..'9' }.take(PinManager.MAX_LENGTH)
                        },
                        label = { Text(label) },
                        singleLine = true,
                        enabled = !isBusy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                message?.let {
                    Text(
                        text = it.toText(),
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
            TextButton(onClick = { onSubmit(values.toList()) }, enabled = !isBusy) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Cancel")
            }
        },
    )
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

private fun PinMessage.toText(): String = when (this) {
    is PinMessage.Wrong -> "Wrong code, $remaining ${if (remaining == 1) "try" else "tries"} left"
    is PinMessage.LockedOut -> "Locked out until ${timeFormatter.format(Instant.ofEpochMilli(untilMs))}"
    PinMessage.InvalidFormat -> "Code must be ${PinManager.MIN_LENGTH}–${PinManager.MAX_LENGTH} digits"
    PinMessage.Mismatch -> "Codes don't match"
    PinMessage.NotSet -> "No code has been set"
    PinMessage.AlreadySet -> "A code is already set"
}
