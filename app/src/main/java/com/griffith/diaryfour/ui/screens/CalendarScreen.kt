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
 * provides a calendar-based view for accessing diary entries.
 * It allows the user to select a specific date and view all entries recorded on that day.
 * The design is state-driven, reacting to changes in the selected date.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavHostController) {
    // The context is necessary for accessing the file system to read diary entries.
    val context = LocalContext.current

    // State variables for the screen. `remember` is used to maintain state across recompositions.
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) } // Nullable, as no date is selected initially.
    var entries by remember { mutableStateOf(listOf<String>()) }
    var showPicker by remember { mutableStateOf(false) } // Controls the visibility of the DatePickerDialog.
    val datePickerState = rememberDatePickerState()

    // `LaunchedEffect` triggers a data fetch from the file system whenever `selectedDate` changes.
    // standard and efficient pattern for handling data loading as a side effect of state changes.
    LaunchedEffect(selectedDate) {
        selectedDate?.let { entries = readDiaryEntries(context.filesDir, "$it.txt") }
    }

    // `Scaffold` provides the standard Material Design layout structure.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diary Calendar") },
                navigationIcon = {
                    // Standard back navigation.
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
            // primary function is to toggle the visibility of the DatePickerDialog.
            Button(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                Spacer(Modifier.width(8.dp))
                Text(selectedDate?.toString() ?: "Select a Date")
            }
            Spacer(Modifier.height(16.dp))

            // `LazyColumn` is used here for efficient display of a potentially long list of diary entries.
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

            // The `DatePickerDialog` is conditionally rendered based on the `showPicker` boolean state.
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
