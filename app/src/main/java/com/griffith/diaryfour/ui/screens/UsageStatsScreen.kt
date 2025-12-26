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
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.LocalDate
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatsScreen(navController: NavHostController) {
    val context = LocalContext.current
    var usageStats by remember { mutableStateOf<Map<LocalDate, Long>>(emptyMap()) }
    var granted by remember { mutableStateOf(hasUsageStatsPermission(context)) }

    // State for the chart data
    var chartEntryModelProducer by remember { mutableStateOf<ChartEntryModelProducer?>(null) }
    var todayUsage by remember { mutableStateOf(0L) }
    var yesterdayUsage by remember { mutableStateOf(0L) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        granted = hasUsageStatsPermission(context)
    }

    LaunchedEffect(granted) {
        if (granted) {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            // Calculate and save today's usage, then read it back along with yesterday's
            val currentTodayUsage = getPhoneUsageForDate(context, today)
            savePhoneUsage(context, today, currentTodayUsage)

            val savedTodayUsage = readPhoneUsageForDate(context, today) ?: 0L
            val savedYesterdayUsage = readPhoneUsageForDate(context, yesterday) ?: 0L

            todayUsage = savedTodayUsage
            yesterdayUsage = savedYesterdayUsage

            // Update the chart model producer with the new data
            chartEntryModelProducer = ChartEntryModelProducer(
                listOf(entryOf(0, savedYesterdayUsage), entryOf(1, savedTodayUsage))
            )

            // Load the full week's stats for the list view
            val pastWeekStats = (0..6).map { i ->
                val date = today.minusDays(i.toLong())
                val dailyUsage = when (date) {
                    today -> savedTodayUsage
                    yesterday -> savedYesterdayUsage
                    else -> readPhoneUsageForDate(context, date)
                }
                date to (dailyUsage ?: 0)
            }.toMap()
            usageStats = pastWeekStats
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phone Usage Stats") },
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
            if (granted) {
                if (chartEntryModelProducer == null || usageStats.isEmpty()) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Calculating usage stats...")
                } else {
                    // Display the new comparison chart
                    UsageComparisonChart(chartEntryModelProducer!!)

                    // Display text summary of the comparison
                    val difference = todayUsage - yesterdayUsage
                    val comparisonText = when {
                        difference > 0 -> "You used your phone ${abs(difference)} minutes more than yesterday."
                        difference < 0 -> "You used your phone ${abs(difference)} minutes less than yesterday."
                        else -> "Your usage was the same as yesterday."
                    }
                    Text(
                        text = comparisonText,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    // Display the weekly total and daily list
                    val weeklyTotal = usageStats.values.sum()
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total This Week", style = MaterialTheme.typography.titleLarge)
                            Text("$weeklyTotal minutes", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(usageStats.keys.sortedDescending().toList()) { date ->
                            val usage = usageStats[date] ?: 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(date.toString(), style = MaterialTheme.typography.bodyLarge)
                                Text("$usage minutes", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            } else {
                // Permission request UI
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

/**
 * A simple bar chart to compare phone usage between yesterday and today.
 */
@Composable
private fun UsageComparisonChart(modelProducer: ChartEntryModelProducer) {
    val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        when (value.toInt()) {
            0 -> "Yesterday"
            1 -> "Today"
            else -> ""
        }
    }
    Chart(
        chart = columnChart(),
        chartModelProducer = modelProducer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisValueFormatter),
        modifier = Modifier.height(200.dp)
    )
}