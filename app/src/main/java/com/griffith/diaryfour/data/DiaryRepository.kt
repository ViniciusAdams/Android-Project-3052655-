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

/**
 * This function is responsible for reading all the diary entries from a given file.
 * It runs on a background thread to avoid blocking the UI, which is important for file I/O.
 * If the file doesn't exist, it simply returns an empty list.
 */
suspend fun readDiaryEntries(filesDir: File, filename: String): List<String> = withContext(Dispatchers.IO) {
    try {
        val file = File(filesDir, filename)
        if (!file.exists()) return@withContext emptyList()
        FileInputStream(file).use { fis ->
            val content = fis.readBytes().toString(Charset.defaultCharset())
            // Our diary entries are separated by a double newline, so we split the file content by that.
            content.split("\n\n").filter { it.isNotBlank() }.map { it.trim() }.reversed()
        }
    } catch (e: Exception) {
        // If something goes wrong, we'll just return an empty list to avoid crashing the app.
        emptyList()
    }
}

/**
 * This function appends a new diary entry to the end of a file.
 * It's designed to be safe and efficient, running on a background thread.
 * If the file doesn't exist, it will be created automatically.
 */
suspend fun appendDiaryEntry(filesDir: File, filename: String, text: String): Boolean = withContext(Dispatchers.IO) {
    try {
        FileOutputStream(File(filesDir, filename), true).use { out ->
            out.write(text.toByteArray())
        }
        true
    } catch (e: Exception) {
        // If we fail to write to the file, we return false to let the caller know.
        false
    }
}

/**
 * This function clears all the content from a diary file, effectively deleting all entries.
 * It's a simple and straightforward way to reset a diary for a given day.
 */
suspend fun clearDiaryFile(filesDir: File, filename: String): Boolean = withContext(Dispatchers.IO) {
    try {
        File(filesDir, filename).writeText("")
        true
    } catch (e: Exception) {
        // If we can't clear the file, we return false.
        false
    }
}

/**
 * A handy utility function to get the current date and time as a nicely formatted string.
 * This is used to timestamp the diary entries.
 */
fun timestampNow(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = now.time.hour.toString().padStart(2, '0')
    val minute = now.time.minute.toString().padStart(2, '0')
    return "${now.date} $hour:$minute"
}
