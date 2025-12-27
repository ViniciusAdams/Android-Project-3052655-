package com.griffith.diaryfour.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.griffith.diaryfour.data.readDiaryEntries
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * This screen is a simple calendar view of the diary entries.
 * The user can pick a date and see all the entries for that day.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavHostController) {
    // We need the context to read the diary entry files.
    val context = LocalContext.current

    // These are our state variables. They hold the currently selected date, the list of entries,
    // and whether or not the date picker is showing.
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var entries by remember { mutableStateOf(listOf<String>()) }
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // This is a side effect that runs whenever the user picks a new date.
    // It reads the diary entries for that date and updates the `entries` state.
    LaunchedEffect(selectedDate) {
        selectedDate?.let { entries = readDiaryEntries(context.filesDir, "$it.txt") }
    }

    // A Scaffold to give our screen a basic layout with a top app bar.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diary Calendar") },
                navigationIcon = {
                    // A simple back button to go back to the menu.
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The button that shows the date picker.
            Button(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                Spacer(Modifier.width(8.dp))
                Text(selectedDate?.toString() ?: "Select a Date")
            }
            Spacer(Modifier.height(16.dp))

            // This is our list of diary entries. It's a LazyColumn, so it's memory-efficient.
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedDate == null) {
                    item { Text("Please select a date to view entries.", modifier = Modifier.padding(top = 24.dp), textAlign = TextAlign.Center) }
                } else if (entries.isEmpty()) {
                    item { Text("No entries for this date.", modifier = Modifier.padding(top = 24.dp), textAlign = TextAlign.Center) }
                } else {
                    items(entries) { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(entry.trim(), modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }

            // The date picker dialog. We only show it when `showPicker` is true.
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
