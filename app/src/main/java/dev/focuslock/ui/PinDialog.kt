package dev.focuslock.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    destructive: Boolean = false,
) {
    val values = remember(fieldLabels) { mutableStateListOf(*Array(fieldLabels.size) { "" }) }

    AppDialog(
        title = title,
        onDismiss = onDismiss,
        confirmLabel = confirmLabel,
        onConfirm = { onSubmit(values.toList()) },
        confirmEnabled = !isBusy,
        dismissEnabled = !isBusy,
        destructive = destructive,
    ) {
        description?.let {
            DialogBody(it)
            Spacer(modifier = Modifier.height(16.dp))
        }
        fieldLabels.forEachIndexed { index, label ->
            if (index > 0) Spacer(modifier = Modifier.height(10.dp))
            CodeField(value = values[index], onValueChange = { values[index] = it }, label = label, enabled = !isBusy)
        }
        DialogStatus(message, isBusy)
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

internal fun PinMessage.toText(): String = when (this) {
    is PinMessage.Wrong -> "Wrong code. $remaining ${if (remaining == 1) "attempt" else "attempts"} remaining."
    is PinMessage.LockedOut -> "Too many attempts. Try again after ${timeFormatter.format(Instant.ofEpochMilli(untilMs))}."
    PinMessage.InvalidFormat -> "Code must be ${PinManager.MIN_LENGTH}\u2013${PinManager.MAX_LENGTH} characters."
    PinMessage.Mismatch -> "Codes don\u2019t match."
    PinMessage.NotSet -> "No code has been set."
    PinMessage.AlreadySet -> "A code is already set."
    PinMessage.Empty -> "Enter the code."
    PinMessage.Failed -> "Something went wrong. Try again."
}
