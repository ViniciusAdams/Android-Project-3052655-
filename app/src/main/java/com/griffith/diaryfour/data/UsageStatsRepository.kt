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


fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // unsafeCheckOpNoThrow returns MODE_ALLOWED when granted
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}


@RequiresApi(Build.VERSION_CODES.O)
fun getPhoneUsageForDate(context: Context, date: LocalDate): Long {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)

    val total = stats?.sumOf { it.totalTimeInForeground } ?: 0L
    return TimeUnit.MILLISECONDS.toMinutes(total)
}


suspend fun savePhoneUsage(context: Context, date: LocalDate, usage: Long) = withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "usage_stats")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val file = File(dir, "$date.txt")
    try {
        FileOutputStream(file).use { it.write(usage.toString().toByteArray()) }
    } catch (e: Exception) {
        // intentionally ignore cache write failures
    }
}


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
