package com.example.callscribe.recording

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object OemAutoStartHelper {

    enum class DeviceBrand {
        XIAOMI,
        SAMSUNG,
        OPPO,
        VIVO,
        HUAWEI,
        OTHER
    }

    fun detectBrand(): DeviceBrand {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> DeviceBrand.XIAOMI
            manufacturer.contains("samsung") -> DeviceBrand.SAMSUNG
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> DeviceBrand.OPPO
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> DeviceBrand.VIVO
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> DeviceBrand.HUAWEI
            else -> DeviceBrand.OTHER
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    fun getRequestBatteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun getOemAutostartIntent(context: Context): Intent? {
        val brand = detectBrand()
        val intentList = when (brand) {
            DeviceBrand.XIAOMI -> listOf(
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT)
            )
            DeviceBrand.OPPO -> listOf(
                Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
                Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
                Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"))
            )
            DeviceBrand.VIVO -> listOf(
                Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
                Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
                Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
            )
            DeviceBrand.HUAWEI -> listOf(
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"))
            )
            DeviceBrand.SAMSUNG -> listOf(
                Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")),
                Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"))
            )
            DeviceBrand.OTHER -> emptyList()
        }

        for (intent in intentList) {
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                return intent
            }
        }
        return null
    }

    fun getBrandInstructions(brand: DeviceBrand): String {
        return when (brand) {
            DeviceBrand.XIAOMI -> "1. Enable 'Autostart' in Security settings.\n2. Set Battery Saver to 'No restrictions'.\n3. Lock CallScribe in Recent Apps."
            DeviceBrand.SAMSUNG -> "1. Open Device Care -> Battery -> Background usage limits.\n2. Add CallScribe to 'Never sleeping apps'.\n3. Disable 'Put unused apps to sleep'."
            DeviceBrand.OPPO -> "1. Open Settings -> Battery -> App Battery Management.\n2. Enable 'Allow Auto-launch' and 'Allow background activity'."
            DeviceBrand.VIVO -> "1. Open iManager -> App Manager -> Autostart manager.\n2. Allow CallScribe to autostart.\n3. In Battery settings, enable 'High background power consumption'."
            DeviceBrand.HUAWEI -> "1. Open Settings -> Battery -> App launch.\n2. Manage CallScribe manually and enable Auto-launch, Secondary launch, and Run in background."
            DeviceBrand.OTHER -> "1. Turn off battery optimization for CallScribe.\n2. Ensure the app has permission to run in background."
        }
    }
}
