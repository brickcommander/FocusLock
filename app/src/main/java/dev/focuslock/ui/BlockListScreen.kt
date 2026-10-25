package dev.focuslock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.focuslock.apps.AppEntry

@Composable
fun BlockListScreen(viewModel: BlockListViewModel) {
    val state by viewModel.uiState.collectAsState()
    val canEdit = !state.isLocked

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text(
                    text = "Block list",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            if (state.isLocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Unlock to edit this list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            state.message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }

            // ── Apps ──
            SectionLabel("Apps")
            GroupCard {
                if (state.apps.isEmpty()) {
                    EmptyRow("No apps blocked")
                } else {
                    state.apps.forEachIndexed { index, app ->
                        if (index > 0) GroupDivider()
                        ItemRow(
                            title = app.label,
                            subtitle = app.packageName.takeIf { it != app.label },
                            canEdit = canEdit,
                            onRemove = { viewModel.removeApp(app.packageName) },
                        )
                    }
                }
            }

            if (canEdit) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Button(
                        onClick = viewModel::openPicker,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                    ) {
                        Text("Add from installed", style = MaterialTheme.typography.titleSmall)
                    }
                }
                AddField(
                    label = "Package name",
                    confirmLabel = "Add",
                    keyboardType = KeyboardType.Uri,
                    enabled = true,
                    onAdd = viewModel::addApp,
                )
            }

            // ── Websites ──
            SectionLabel("Websites")
            Text(
                text = "Blocked in Chrome and Brave. All subdomains included.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            GroupCard {
                if (state.sites.isEmpty()) {
                    EmptyRow("No websites blocked")
                } else {
                    state.sites.forEachIndexed { index, site ->
                        if (index > 0) GroupDivider()
                        ItemRow(
                            title = site,
                            subtitle = null,
                            canEdit = canEdit,
                            onRemove = { viewModel.removeSite(site) },
                        )
                    }
                }
            }

            if (canEdit) {
                AddField(
                    label = "Domain, e.g. tiktok.com",
                    confirmLabel = "Add",
                    keyboardType = KeyboardType.Uri,
                    enabled = true,
                    onAdd = viewModel::addSite,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (state.showPicker) {
        AppPickerDialog(
            apps = state.pickerApps,
            onPick = viewModel::addApp,
            onDismiss = viewModel::closePicker,
        )
    }
}

// ── Shared ──

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun GroupCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        content()
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun EmptyRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Composable
private fun ItemRow(title: String, subtitle: String?, canEdit: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (canEdit) {
            TextButton(onClick = onRemove) {
                Text(
                    "Remove",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AddField(
    label: String,
    confirmLabel: String,
    keyboardType: KeyboardType,
    enabled: Boolean,
    onAdd: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(label) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = {
                onAdd(text)
                text = ""
            },
            enabled = enabled && text.isNotBlank(),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(confirmLabel)
        }
    }
}

@Composable
private fun AppPickerDialog(apps: List<AppEntry>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add app", style = MaterialTheme.typography.headlineSmall) },
        text = {
            if (apps.isEmpty()) {
                Text("No other apps available.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(app.packageName) }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(app.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(16.dp),
    )
}
