package com.griffith.diaryfour.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * A helper function to check if we have the "Usage Stats" permission.
 * This is a special permission that the user has to grant from the device settings.
 */
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    // AppOpsManager is the way to go for checking this permission.
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * This function queries the system for the total phone usage for a given date.
 * It requires the "Usage Stats" permission to be granted.
 * The result is returned in minutes.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun getPhoneUsageForDate(context: Context, date: LocalDate): Long {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    // We need to define a time range to query the usage stats.
    val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)

    // We sum up the total time in the foreground for all apps.
    val total = stats?.sumOf { it.totalTimeInForeground } ?: 0L
    return TimeUnit.MILLISECONDS.toMinutes(total)
}

/**
 * This function saves the phone usage for a given date to a file.
 * This is useful because querying the usage stats can be slow, so we cache the results.
 */
suspend fun savePhoneUsage(context: Context, date: LocalDate, usage: Long) = withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "usage_stats")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val file = File(dir, "$date.txt")
    try {
        FileOutputStream(file).use { it.write(usage.toString().toByteArray()) }
    } catch (e: Exception) {
        // If something goes wrong, we'll just log the error and continue.
    }
}

/**
 * This function reads the cached phone usage for a given date from a file.
 * If the file doesn't exist, it returns null.
 */
suspend fun readPhoneUsageForDate(context: Context, date: LocalDate): Long? = withContext(Dispatchers.IO) {
    val file = File(File(context.filesDir, "usage_stats"), "$date.txt")
    if (!file.exists()) return@withContext null

    return@withContext try {
        FileInputStream(file).use { fis ->
            fis.readBytes().toString(Charset.defaultCharset()).toLong()
        }
    } catch (e: Exception) {
        null
    }
}
