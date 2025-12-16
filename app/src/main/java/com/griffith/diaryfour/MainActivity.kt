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
 * A screen that displays a calendar and shows diary entries for the selected date.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavHostController) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var entries by remember { mutableStateOf(listOf<String>()) }
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // When a date is selected, load the diary entries for that date.
    LaunchedEffect(selectedDate) {
        selectedDate?.let { entries = readDiaryEntries(context.filesDir, "$it.txt") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diary Calendar") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                Spacer(Modifier.width(8.dp))
                Text(selectedDate?.toString() ?: "Select a Date")
            }
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedDate == null) {
                    item {
                        Text(
                            "Please select a date to view entries.",
                            modifier = Modifier.padding(top = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (entries.isEmpty()) {
                    item {
                        Text(
                            "No entries for this date.",
                            modifier = Modifier.padding(top = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(entries) { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(entry.trim(), modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }

            if (showPicker) {
                DatePickerDialog(
                    onDismissRequest = { showPicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showPicker = false
                                datePickerState.selectedDateMillis?.let {
                                    selectedDate = Instant.ofEpochMilli(it)
                                        .atZone(ZoneId.systemDefault()).toLocalDate()
                                }
                            },
                            enabled = datePickerState.selectedDateMillis != null
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPicker = false }) { Text("Cancel") }
                    }
                ) { DatePicker(state = datePickerState) }
            }
        }
    }
}