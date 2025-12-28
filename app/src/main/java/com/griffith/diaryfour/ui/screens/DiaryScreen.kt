package com.griffith.diaryfour.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.griffith.diaryfour.data.appendDiaryEntry
import com.griffith.diaryfour.data.clearDiaryFile
import com.griffith.diaryfour.data.readDiaryEntries
import com.griffith.diaryfour.data.timestampNow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 *  provides the core journaling functionality.
 * It allows for the creation, display, and management of diary entries for a user-selected date.
 * The architecture is centered around a stateful composable that reacts to user input and data changes.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(navController: NavHostController, initialDate: LocalDate) {
    // context is required for file system access, while the coroutine scope is used for launching background tasks.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State variables are managed here. used to ensure state is preserved across recompositions.
    var selectedDate by remember { mutableStateOf(initialDate) }
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    var entries by remember { mutableStateOf(listOf<String>()) }
    var message by remember { mutableStateOf<String?>(null) } // Used to display transient feedback to the user.
    var showPicker by remember { mutableStateOf(false) } // Controls the visibility of the date picker dialog.
    val datePickerState = rememberDatePickerState()

    // A `LaunchedEffect` is used to trigger a data fetch whenever the `selectedDate` state changes.
    // key Compose pattern for handling side effects in a controlled manmer.
    LaunchedEffect(selectedDate) {
        entries = readDiaryEntries(context.filesDir, "${selectedDate}.txt")
    }

    // effect manages the display of transient user feedback messages.
    // automatically dismisses the message after a 2-second delay.
    LaunchedEffect(message) {
        if (message != null) {
            delay(2000)
            message = null
        }
    }

    // `Scaffold` provides the basic Material Design layout structure for the screen.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diary") },
                navigationIcon = {
                    // A standard back navigation icon.
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()
        ) {
            // button triggers the display of the date picker dialog.
            Button(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                Spacer(Modifier.width(8.dp))
                Text(selectedDate.toString())
            }

            // A `LazyColumn` is used for displaying the list of entries to ensure efficient rendering of long lists.
            LazyColumn(
                modifier = Modifier.weight(1f), // `weight(1f)` make the list expand to fill available space.
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true // displays the newest entries at the bottom, which is a common pattern for chat-like interfaces.
            ) {
                if (entries.isEmpty()) {
                    item { Text("No entries for this date.", modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), textAlign = TextAlign.Center) }
                } else {
                    items(entries) { entry ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text(entry.trim(), modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }

            // A temporary message that provides feedback on save or delete operations.
            AnimatedVisibility(visible = message != null) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer, tonalElevation = 4.dp) {
                    Text(text = message ?: "", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            // This section contains the text input field and action buttons.
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    label = { Text("Write your entry...") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp, max = 160.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // This button provides a way to clear all entries for the selected date.
                    FilledTonalButton(onClick = {
                        scope.launch {
                            clearDiaryFile(context.filesDir, "${selectedDate}.txt")
                            entries = emptyList()
                            message = "Diary cleared"
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear All")
                        Spacer(Modifier.width(8.dp))
                        Text("Clear All")
                    }
                    // The primary action button for saving a new diary entry.
                    Button(
                        onClick = {
                            val content = textState.text.trim()
                            if (content.isNotEmpty()) {
                                val entry = "${timestampNow()} — $content\n\n"
                                // File I/O is performed in a background coroutine.
                                scope.launch {
                                    if (appendDiaryEntry(context.filesDir, "${selectedDate}.txt", entry)) {
                                        // After a successful save, the UI is updated to reflect the new state.
                                        entries = readDiaryEntries(context.filesDir, "${selectedDate}.txt")
                                        textState = TextFieldValue("")
                                        message = "Entry Saved!"
                                    }
                                }
                            }
                        },
                        enabled = textState.text.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }

            // The `DatePickerDialog` is conditionally displayed based on the `showPicker` state.
            if (showPicker) {
                DatePickerDialog(
                    onDismissRequest = { showPicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showPicker = false
                                datePickerState.selectedDateMillis?.let {
                                    selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                }
                            },
                            enabled = datePickerState.selectedDateMillis != null
                        ) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
                ) { DatePicker(state = datePickerState) }
            }
        }
    }
}
