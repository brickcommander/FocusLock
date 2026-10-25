package dev.focuslock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.focuslock.setup.SetupStatus

@Composable
fun SetupChecklist(
    status: SetupStatus,
    onOpenAccessibilitySettings: () -> Unit,
    onActivateDeviceAdmin: () -> Unit,
    onSetCode: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Setup", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        ChecklistItem(
            done = status.blockerServiceEnabled,
            title = "1. Turn on blocker",
            actionLabel = "Open Accessibility settings",
            onAction = onOpenAccessibilitySettings,
            help = "If the toggle is greyed out: App info → ⋮ → Allow restricted settings.",
        )
        ChecklistItem(
            done = status.deviceAdminActive,
            title = "2. Turn on uninstall protection",
            actionLabel = "Activate",
            onAction = onActivateDeviceAdmin,
        )
        ChecklistItem(
            done = status.pinSet,
            title = "3. Friend sets the code",
            actionLabel = "Hand phone to friend",
            onAction = onSetCode,
            help = "They set the code that unlocks FocusLock. Don't watch them type it.",
        )
    }
}

@Composable
private fun ChecklistItem(
    done: Boolean,
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    help: String? = null,
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (done) "✅" else "❌", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        if (!done) {
            OutlinedButton(onClick = onAction, modifier = Modifier.padding(start = 32.dp)) {
                Text(actionLabel)
            }
            help?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp),
                )
            }
        }
    }
}
