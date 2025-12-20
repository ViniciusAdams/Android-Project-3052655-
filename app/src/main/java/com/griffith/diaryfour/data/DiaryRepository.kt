package com.griffith.diaryfour.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset

suspend fun readDiaryEntries(filesDir: File, filename: String): List<String> = withContext(Dispatchers.IO) {
    try {
        val file = File(filesDir, filename)
        if (!file.exists()) return@withContext emptyList()
        FileInputStream(file).use { fis ->
            val content = fis.readBytes().toString(Charset.defaultCharset())
            content.split("\n\n").filter { it.isNotBlank() }.map { it.trim() }.reversed()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun appendDiaryEntry(filesDir: File, filename: String, text: String): Boolean = withContext(Dispatchers.IO) {
    try {
        FileOutputStream(File(filesDir, filename), true).use { out ->
            out.write(text.toByteArray())
        }
        true
    } catch (e: Exception) {
        false
    }
}

suspend fun clearDiaryFile(filesDir: File, filename: String): Boolean = withContext(Dispatchers.IO) {
    try {
        File(filesDir, filename).writeText("")
        true
    } catch (e: Exception) {
        false
    }
}

fun timestampNow(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = now.time.hour.toString().padStart(2, '0')
    val minute = now.time.minute.toString().padStart(2, '0')
    return "${now.date} $hour:$minute"
}
