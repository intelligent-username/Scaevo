package com.scaevo.ui.blocklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scaevo.data.db.entity.BlockedApp
import com.scaevo.ui.components.AppLimitProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistScreen(viewModel: BlocklistViewModel) {
    val blockedApps by viewModel.blockedApps.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val liveUsages by viewModel.todayLiveUsages.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var selectedAppForLimit by remember { mutableStateOf<BlockedApp?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Apps") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add app")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (blockedApps.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No apps blocked", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap + to add apps you want to block.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(blockedApps, key = { it.packageName }) { app ->
                    ListItem(
                        headlineContent = { Text(app.appLabel) },
                        supportingContent = {
                            Column {
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (app.dailyLimitMinutes != null) {
                                    Spacer(Modifier.height(4.dp))
                                    AppLimitProgressBar(
                                        usedMs = liveUsages[app.packageName] ?: 0L,
                                        limitMinutes = app.dailyLimitMinutes,
                                        modifier = Modifier.clickable { selectedAppForLimit = app }
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (app.dailyLimitMinutes == null) {
                                    TextButton(onClick = { selectedAppForLimit = app }) {
                                        Text("Set Limit", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Switch(
                                    checked = app.isEnabled,
                                    onCheckedChange = { viewModel.toggleEnabled(app) }
                                )
                                IconButton(onClick = { viewModel.removeFromBlocklist(app.packageName) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }

        if (showPicker) {
            AppPickerDialog(
                apps = installedApps.filter { (pkg, _) ->
                    blockedApps.none { it.packageName == pkg }
                },
                onSelect = { (pkg, label) ->
                    viewModel.addToBlocklist(pkg, label)
                    showPicker = false
                },
                onDismiss = { showPicker = false }
            )
        }

        if (selectedAppForLimit != null) {
            var limitInput by remember { 
                mutableStateOf(selectedAppForLimit?.dailyLimitMinutes?.toString() ?: "") 
            }
            AlertDialog(
                onDismissRequest = { selectedAppForLimit = null },
                title = { Text("Set Daily Limit") },
                text = {
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        label = { Text("Limit in minutes") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val minutes = limitInput.toIntOrNull()
                        viewModel.updateLimit(selectedAppForLimit!!, minutes)
                        selectedAppForLimit = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.updateLimit(selectedAppForLimit!!, null)
                        selectedAppForLimit = null
                    }) {
                        Text("Remove Limit")
                    }
                }
            )
        }
    }
}

@Composable
fun AppPickerDialog(
    apps: List<Pair<String, String>>,
    onSelect: (Pair<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App to Block") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(apps) { app ->
                    TextButton(
                        onClick = { onSelect(app) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            app.second,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
