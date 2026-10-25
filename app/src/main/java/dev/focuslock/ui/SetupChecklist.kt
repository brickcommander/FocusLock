package dev.focuslock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.focuslock.setup.SetupStatus
import dev.focuslock.ui.theme.LocalStatusColors

@Composable
fun SetupChecklist(
    status: SetupStatus,
    onOpenAccessibilitySettings: () -> Unit,
    onActivateDeviceAdmin: () -> Unit,
    onSetCode: () -> Unit,
) {
    GroupCard {
        StepRow(
            step = 1,
            done = status.blockerServiceEnabled,
            title = "Enable the blocker",
            help = "If it's greyed out: App info \u2192 \u22EE \u2192 Allow restricted settings",
            actionLabel = "Open",
            onAction = onOpenAccessibilitySettings,
        )
        RowDivider()
        StepRow(
            step = 2,
            done = status.deviceAdminActive,
            title = "Uninstall protection",
            help = "Stops FocusLock from being removed while locked",
            actionLabel = "Activate",
            onAction = onActivateDeviceAdmin,
        )
        RowDivider()
        StepRow(
            step = 3,
            done = status.pinSet,
            title = "Set the code",
            help = "Hand your phone to a friend. They choose it.",
            actionLabel = "Set",
            onAction = onSetCode,
        )
    }
}

@Composable
private fun StepRow(step: Int, done: Boolean, title: String, help: String, actionLabel: String, onAction: () -> Unit) {
    val status = LocalStatusColors.current
    ListRow(
        title = title,
        subtitle = if (done) "Done" else help,
        leading = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (done) status.freeTint else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (done) status.freeTint else MaterialTheme.colorScheme.outlineVariant, CircleShape),
            ) {
                Text(
                    text = if (done) "\u2713" else "$step",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (done) status.free else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailing = if (done) null else {
            { TextButton(onClick = onAction) { Text(actionLabel, style = MaterialTheme.typography.titleSmall) } }
        },
    )
}
