package com.griffith.diaryfour.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.griffith.diaryfour.data.getPhoneUsageForDate
import com.griffith.diaryfour.data.hasUsageStatsPermission
import com.griffith.diaryfour.data.readPhoneUsageForDate
import com.griffith.diaryfour.data.savePhoneUsage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * This screen is all about showing the user how much they're using their phone.
 * It requires a special permission, so we have to handle the case where the user hasn't granted it yet.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatsScreen(navController: NavHostController) {
    // We need the context to check for the permission and to get the usage stats.
    val context = LocalContext.current

    // These are our state variables. `usageStats` holds the data for the last week,
    // and `granted` tells us if we have the permission to get that data.
    var usageStats by remember { mutableStateOf<Map<LocalDate, Long>>(emptyMap()) }
    var granted by remember { mutableStateOf(hasUsageStatsPermission(context)) }

    // This is the modern way to handle activity results in Compose. We use it to re-check the permission
    // after the user has been to the settings screen.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        granted = hasUsageStatsPermission(context)
    }

    // This is a side effect that runs whenever the `granted` state changes.
    // If the user has granted the permission, we load the usage stats for the last week.
    LaunchedEffect(granted) {
        if (granted) {
            val today = LocalDate.now()
            val usage = getPhoneUsageForDate(context, today)
            savePhoneUsage(context, today, usage)

            // We want to show the stats for the last 7 days, so we loop back from today.
            val pastWeekStats = (0..6).map { i ->
                val date = today.minusDays(i.toLong())
                val dailyUsage = readPhoneUsageForDate(context, date)
                date to (dailyUsage ?: 0)
            }.toMap()

            usageStats = pastWeekStats
        }
    }

    // A Scaffold to give our screen some structure.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phone Usage Stats") },
                navigationIcon = {
                    // The back button.
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
            if (granted) {
                // If we're still loading the stats, we show a progress indicator.
                if (usageStats.isEmpty()) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Calculating usage stats...")
                } else {
                    // The total usage for the week. A nice, big number to grab the user's attention.
                    val weeklyTotal = usageStats.values.sum()

                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total This Week", style = MaterialTheme.typography.titleLarge)
                            Text("$weeklyTotal minutes", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // The bar chart. This is a great way to visualize the data.
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                        Text("Weekly Usage Breakdown", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        BarChart(
                            data = usageStats.entries.associate {
                                // We use the short day of the week as the label for the bar chart.
                                it.key.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to it.value.toFloat()
                            },
                            barColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    // And finally, a list of the daily usage stats.
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(usageStats.keys.sortedDescending().toList()) { date ->
                            val usage = usageStats[date] ?: 0
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(date.format(DateTimeFormatter.ofPattern("MMM dd")), style = MaterialTheme.typography.bodyLarge)
                                Text("$usage minutes", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            } else {
                // If the user hasn't granted the permission, we show them a message and a button to go to the settings screen.
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Permission is required to calculate usage time.", textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { launcher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}
