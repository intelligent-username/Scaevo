package com.scaevo.ui.utils

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.scaevo.blocking.AppBlockingAccessibilityService

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
