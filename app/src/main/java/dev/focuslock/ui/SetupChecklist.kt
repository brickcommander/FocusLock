package dev.focuslock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.focuslock.setup.SetupStatus
import dev.focuslock.ui.theme.Teal400
import dev.focuslock.ui.theme.Teal900

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
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(20.dp),
    ) {
        Text(
            text = "GET STARTED",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(14.dp))

        StepItem(
            step = 1,
            done = status.blockerServiceEnabled,
            title = "Turn on blocker",
            actionLabel = "Open Accessibility settings",
            onAction = onOpenAccessibilitySettings,
            help = "If the toggle is greyed out:\nApp info → ⋮ → Allow restricted settings.",
        )
        Spacer(modifier = Modifier.height(10.dp))
        StepItem(
            step = 2,
            done = status.deviceAdminActive,
            title = "Turn on uninstall protection",
            actionLabel = "Activate",
            onAction = onActivateDeviceAdmin,
        )
        Spacer(modifier = Modifier.height(10.dp))
        StepItem(
            step = 3,
            done = status.pinSet,
            title = "Friend sets the code",
            actionLabel = "Hand phone to friend",
            onAction = onSetCode,
            help = "They set the code that unlocks FocusLock.\nDon't watch them type it.",
        )
    }
}

@Composable
private fun StepItem(
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
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (done) Teal400.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
            .border(
                width = 1.dp,
                color = if (done) Teal400.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Step indicator
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (done) Teal400 else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        ) {
            Text(
                text = if (done) "✓" else "$step",
                color = if (done) Teal900 else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (!done) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.height(36.dp),
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                }
                help?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
