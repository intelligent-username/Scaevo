package com.scaevo.ui.blocklist

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Search
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scaevo.data.db.entity.BlockedApp
import com.scaevo.ui.components.AppLimitProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistScreen(viewModel: BlocklistViewModel) {
    val blockedApps by viewModel.blockedApps.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val liveUsages by viewModel.todayLiveUsages.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var selectedAppForLimit by remember { mutableStateOf<BlockedApp?>(null) }
    var blockedQuery by remember { mutableStateOf("") }
    val filteredBlockedApps = remember(blockedApps, blockedQuery) {
        val normalized = blockedQuery.trim()
        if (normalized.isEmpty()) blockedApps
        else blockedApps.filter {
            it.appLabel.contains(normalized, ignoreCase = true) ||
                it.packageName.contains(normalized, ignoreCase = true)
        }
    }

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
                item {
                    OutlinedTextField(
                        value = blockedQuery,
                        onValueChange = { blockedQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Search blocked apps") }
                    )
                }

                items(filteredBlockedApps, key = { it.packageName }) { app ->
                    val appIcon = remember(app.packageName) {
                        runCatching {
                            context.packageManager.getApplicationIcon(app.packageName)
                                .toBitmap(80, 80)
                                .asImageBitmap()
                        }.getOrNull()
                    }

                    ListItem(
                        leadingContent = {
                            if (appIcon != null) {
                                Image(
                                    bitmap = appIcon,
                                    contentDescription = "${app.appLabel} icon",
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        },
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
            val parsedMinutes = limitInput.toIntOrNull()
            val isValidLimit = parsedMinutes != null && parsedMinutes > 0

            AlertDialog(
                onDismissRequest = { selectedAppForLimit = null },
                title = { Text("Set Daily Limit") },
                text = {
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        label = { Text("Limit in minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = limitInput.isNotEmpty() && !isValidLimit,
                        supportingText = {
                            if (limitInput.isNotEmpty() && !isValidLimit) {
                                Text("Enter a positive number")
                            }
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateLimit(selectedAppForLimit!!, parsedMinutes)
                            selectedAppForLimit = null
                        },
                        enabled = isValidLimit
                    ) {
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
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        val normalized = query.trim()
        if (normalized.isEmpty()) apps
        else apps.filter { (_, label) -> label.contains(normalized, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App to Block") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search apps") }
                )

                Spacer(Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(filteredApps, key = { it.first }) { app ->
                        val appIcon = remember(app.first) {
                            runCatching {
                                context.packageManager.getApplicationIcon(app.first)
                                    .toBitmap(72, 72)
                                    .asImageBitmap()
                            }.getOrNull()
                        }

                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(app) },
                            leadingContent = {
                                if (appIcon != null) {
                                    Image(
                                        bitmap = appIcon,
                                        contentDescription = "${app.second} icon",
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    app.second,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
