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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.focuslock.apps.AppEntry

@Composable
fun BlockListScreen(viewModel: BlockListViewModel) {
    val state by viewModel.uiState.collectAsState()
    val canEdit = !state.isLocked

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Block list", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))

            if (state.isLocked) {
                Text(
                    text = "Locked: unlock with the code to change the list.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text("Apps", style = MaterialTheme.typography.titleLarge)
            if (state.apps.isEmpty()) Text("No apps blocked", style = MaterialTheme.typography.bodyMedium)
            state.apps.forEach { app ->
                ListRow(
                    title = app.label,
                    subtitle = app.packageName.takeIf { it != app.label },
                    canEdit = canEdit,
                    onRemove = { viewModel.removeApp(app.packageName) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = viewModel::openPicker, enabled = canEdit) {
                Text("Add from installed apps")
            }
            AddField(
                label = "Or type a package name",
                confirmLabel = "Add app",
                keyboardType = KeyboardType.Uri,
                enabled = canEdit,
                onAdd = viewModel::addApp,
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Websites", style = MaterialTheme.typography.titleLarge)
            Text(
                "Blocked in Chrome and Brave, including all subdomains.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.sites.isEmpty()) Text("No websites blocked", style = MaterialTheme.typography.bodyMedium)
            state.sites.forEach { site ->
                ListRow(title = site, subtitle = null, canEdit = canEdit, onRemove = { viewModel.removeSite(site) })
            }
            AddField(
                label = "Website, e.g. tiktok.com",
                confirmLabel = "Add website",
                keyboardType = KeyboardType.Uri,
                enabled = canEdit,
                onAdd = viewModel::addSite,
            )
        }
    }

    if (state.showPicker) {
        AppPickerDialog(apps = state.pickerApps, onPick = viewModel::addApp, onDismiss = viewModel::closePicker)
    }
}

@Composable
private fun ListRow(title: String, subtitle: String?, canEdit: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (canEdit) {
            TextButton(onClick = onRemove) { Text("Remove") }
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
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                onAdd(text)
                text = ""
            },
            enabled = enabled && text.isNotBlank(),
        ) {
            Text(confirmLabel)
        }
    }
}

@Composable
private fun AppPickerDialog(apps: List<AppEntry>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an app to block") },
        text = {
            if (apps.isEmpty()) {
                Text("No other apps to add.")
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
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}