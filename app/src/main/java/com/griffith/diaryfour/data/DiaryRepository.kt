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
 * handles the reading of diary entries from the file system.
 * It operates on a background thread using `withContext(Dispatchers.IO)` to prevent blocking the UI.
 * If the specified file does not exist, it returns an empty list.
 */
suspend fun readDiaryEntries(filesDir: File, filename: String): List<String> = withContext(Dispatchers.IO) {
    try {
        val file = File(filesDir, filename)
        if (!file.exists()) return@withContext emptyList()
        FileInputStream(file).use { fis ->
            val content = fis.readBytes().toString(Charset.defaultCharset())
            // The file content is parsed by splitting it by a double newline, which delimits each entry.
            content.split("\n\n").filter { it.isNotBlank() }.map { it.trim() }.reversed()
        }
    } catch (e: Exception) {
        // In the case of an I/O error, an empty list is returned to ensure the app remains stable.
        emptyList()
    }
}

/**
 * appends a new diary entry to a specified file.
 * The operation is performed on a background thread. If the file does not exist, it is created automatically.
 * @return `true` if the write operation is successful,`false` otherwisee.
 */
suspend fun appendDiaryEntry(filesDir: File, filename: String, text: String): Boolean = withContext(Dispatchers.IO) {
    try {
        FileOutputStream(File(filesDir, filename), true).use { out ->
            out.write(text.toByteArray())
        }
        true
    } catch (e: Exception) {
        // A failure in the write operation is signaled by returning `false`.
        false
    }
}

/**
 * clears all content from a diary file.
 * It is the primary mechanism for deleting all entries for a specific date.
 * @return `true` if the file is successfully cleared, `false` otherwise.
 */
suspend fun clearDiaryFile(filesDir: File, filename: String): Boolean = withContext(Dispatchers.IO) {
    try {
        File(filesDir, filename).writeText("")
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * A utility function that generates a formatted timestamp for the current moment.
 * used to prefix each new diary entry.
 */
fun timestampNow(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = now.time.hour.toString().padStart(2, '0')
    val minute = now.time.minute.toString().padStart(2, '0')
    return "${now.date} $hour:$minute"
}
