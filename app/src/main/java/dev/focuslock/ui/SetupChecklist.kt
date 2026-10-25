package dev.focuslock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.focuslock.setup.SetupStatus
import dev.focuslock.ui.theme.StatusLocked
import dev.focuslock.ui.theme.StatusUnlocked

@Composable
fun SetupChecklist(
    status: SetupStatus,
    onOpenAccessibilitySettings: () -> Unit,
    onActivateDeviceAdmin: () -> Unit,
    onSetCode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        StepRow(
            step = 1,
            done = status.blockerServiceEnabled,
            title = "Enable blocker service",
            actionLabel = "Open settings",
            onAction = onOpenAccessibilitySettings,
            help = "If greyed out: App info \u2192 \u22EE \u2192 Allow restricted settings",
        )
        StepDivider()
        StepRow(
            step = 2,
            done = status.deviceAdminActive,
            title = "Uninstall protection",
            actionLabel = "Activate",
            onAction = onActivateDeviceAdmin,
        )
        StepDivider()
        StepRow(
            step = 3,
            done = status.pinSet,
            title = "Set the code",
            actionLabel = "Set code",
            onAction = onSetCode,
            help = "Hand your phone to a friend. They choose the code.",
        )
    }
}

@Composable
private fun StepRow(
    step: Int,
    done: Boolean,
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    help: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Status indicator
        Text(
            text = if (done) "\u2713" else "$step",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (done) StatusUnlocked else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (!done) {
                help?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (!done) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun StepDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
