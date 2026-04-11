package com.justspent.service

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

object ServiceController {

    /**
     * Checks if the app has the PACKAGE_USAGE_STATS permission.
     */
    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Starts the UsageMonitorService.
     */
    fun startMonitoring(context: Context) {
        if (hasUsageAccessPermission(context)) {
            val intent = Intent(context, UsageMonitorService::class.java)
            context.startForegroundService(intent)
        }
    }

    /**
     * Stops the UsageMonitorService.
     */
    fun stopMonitoring(context: Context) {
        val intent = Intent(context, UsageMonitorService::class.java)
        context.stopService(intent)
    }
}
