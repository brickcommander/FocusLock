package dev.focuslock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
fun BlockListScreen(viewModel: BlockListViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val canEdit = !state.isLocked

    AppScreen(
        title = "Block list",
        subtitle = "Blocked while FocusLock is locked or a focus session runs.",
        onBack = onBack,
    ) {
        if (state.isLocked) {
            Banner(title = "View only", body = "Unlock to change this list. Not possible during a focus session.")
        }
        state.message?.let { InfoText(it) }

        SectionLabel("Apps \u00B7 ${state.apps.size}")
        GroupCard {
            if (state.apps.isEmpty()) EmptyRow("No apps blocked")
            state.apps.forEachIndexed { index, app ->
                if (index > 0) RowDivider()
                ListRow(
                    title = app.label,
                    subtitle = app.packageName.takeIf { it != app.label },
                    leading = { Monogram(app.label) },
                    trailing = if (canEdit) {
                        { RemoveButton { viewModel.removeApp(app.packageName) } }
                    } else null,
                )
            }
            if (canEdit) {
                RowDivider()
                ListRow(title = "Add an app", onClick = viewModel::openPicker, leading = { Monogram("+") })
            }
        }
        if (canEdit) {
            AddField(label = "Or type a package name", keyboardType = KeyboardType.Uri, onAdd = viewModel::addApp)
        }

        SectionLabel("Websites \u00B7 ${state.sites.size}")
        GroupCard {
            if (state.sites.isEmpty()) EmptyRow("No websites blocked")
            state.sites.forEachIndexed { index, site ->
                if (index > 0) RowDivider()
                ListRow(
                    title = site,
                    subtitle = "and all its subdomains",
                    trailing = if (canEdit) {
                        { RemoveButton { viewModel.removeSite(site) } }
                    } else null,
                )
            }
        }
        Text(
            text = "Blocked in Chrome and Brave.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = ScreenPadding + 4.dp, vertical = 8.dp),
        )
        if (canEdit) {
            AddField(label = "Add a website, e.g. tiktok.com", keyboardType = KeyboardType.Uri, onAdd = viewModel::addSite)
        }
    }

    if (state.showPicker) {
        AppPickerDialog(apps = state.pickerApps, onPick = viewModel::addApp, onDismiss = viewModel::closePicker)
    }
}

@Composable
private fun RemoveButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text("Remove", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AddField(label: String, keyboardType: KeyboardType, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(label, style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(10.dp))
        SecondaryButton(
            text = "Add",
            onClick = {
                onAdd(text)
                text = ""
            },
            enabled = text.isNotBlank(),
        )
    }
}

@Composable
fun AppPickerDialog(apps: List<AppEntry>, onPick: (String) -> Unit, onDismiss: () -> Unit, title: String = "Choose an app") {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            if (apps.isEmpty()) {
                DialogBody("No other apps available.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(app.packageName) }
                                .padding(vertical = 10.dp),
                        ) {
                            Monogram(app.label)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(app.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
    )
}
