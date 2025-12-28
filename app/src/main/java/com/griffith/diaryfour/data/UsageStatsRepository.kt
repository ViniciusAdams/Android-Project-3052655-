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
 *  check if the app has been granted the `GET_USAGE_STATS` permission.
 * special permission that requires the user to grant access through the system settings.
 */
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    // The AppOpsManager is used to check the mode of the specific permission.
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * queries the system's `UsageStatsManager` to get the total phone usage for a specific date.
 * It requires the `GET_USAGE_STATS` permission. The returned value is in minutes.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun getPhoneUsageForDate(context: Context, date: LocalDate): Long {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    // A time range for the query must be defined, from the start to the end of the given date.
    val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)

    // The total time in the foreground for all apps is summed up to get the total usage.
    val total = stats?.sumOf { it.totalTimeInForeground } ?: 0L
    return TimeUnit.MILLISECONDS.toMinutes(total)
}

/**
 * saves the phone usage data to a local file.
 * caching strategy is used to avoid the performance cost of repeatedly querying the `UsageStatsManager`.
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
        // Errors during this file operation are not critical, so they are not propagated.
    }
}

/**
 * function reads the cached phone usage data from a local file.
 * If the file for the given date does not exist, it returns null.
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
