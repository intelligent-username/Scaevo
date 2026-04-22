package com.scaevo.ui.permissions

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.scaevo.blocking.AppBlockingAccessibilityService
import com.scaevo.data.usage.UsageStatsHelper

@Composable
fun PermissionsScreen(
    onAllGranted: () -> Unit,
    usageStatsHelper: UsageStatsHelper
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasUsage by remember { mutableStateOf(false) }
    var hasAccessibility by remember { mutableStateOf(false) }
    var hasBatteryExemption by remember { mutableStateOf(false) }

    fun recheck() {
        hasUsage = usageStatsHelper.hasUsagePermission()
        hasAccessibility = isAccessibilityServiceEnabled(context)
        hasBatteryExemption = isBatteryOptimizationIgnored(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) recheck()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasUsage, hasAccessibility) {
        if (hasUsage && hasAccessibility) onAllGranted()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 48.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Welcome to Scaevo",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
            Text(
                text = "To track and block apps reliably, Scaevo needs a few permissions.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        item {
            PermissionCard(
            title = "Usage Access",
            description = "Allows reading per-app screen time from the OS. Navigate to your app in the list and enable it.",
            granted = hasUsage,
            required = true,
            onGrant = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        )

        }

        item {
            PermissionCard(
            title = "Accessibility Service",
            description = "Required to detect when blocked apps open and redirect to home screen.",
            granted = hasAccessibility,
            required = true,
            onGrant = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        )

        }

        item {
            PermissionCard(
            title = "Battery Optimization Exemption",
            description = "Prevents the OS from killing the blocking service. Recommended.",
            granted = hasBatteryExemption,
            required = false,
            onGrant = {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            }
        )

        }

        // Samsung-specific prompt (Phase 2)
        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            item {
                PermissionCard(
                title = "Samsung: Never Sleeping Apps",
                description = "Settings → Battery → Background usage limits → Never sleeping apps → add Scaevo. Required for blocking to remain active after the screen locks.",
                granted = false, // No reliable detection method exists
                required = false,
                onGrant = {
                    try {
                        context.startActivity(
                            Intent("com.samsung.android.lool.ACTION_DEVICE_CARE_MAIN")
                        )
                    } catch (_: android.content.ActivityNotFoundException) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            )
        }
    }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            if (hasUsage && hasAccessibility) {
                Button(
                    onClick = onAllGranted,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Continue", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Button(
                    onClick = { /* Do nothing */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = false
                ) {
                    Text("Grant required to continue", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    required: Boolean,
    onGrant: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (required && !granted) {
                        Spacer(Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error
                        ) {
                            Text("Required", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (granted) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Button(
                    onClick = onGrant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant")
                }
            }
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val componentName = ComponentName(context, AppBlockingAccessibilityService::class.java)
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.split(":").any {
        ComponentName.unflattenFromString(it) == componentName
    }
}

fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
