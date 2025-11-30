package com.griffith.diaryfour

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

// --- NAVIGATION --- //

/**
 * Available screens in the app and their routes.
 */
sealed class Screen(val route: String) {
    object Menu : Screen("menu")
    object Diary : Screen("diary/{date}") {
        fun createRoute(date: LocalDate) = "diary/$date"
    }
    object Calendar : Screen("calendar")
    object UsageStats : Screen("usage_stats")
}

// --- ACTIVITY --- //

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Prefer dynamic colors when available (Android 12+), otherwise fall back.
            val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val ctx = LocalContext.current
                dynamicDarkColorScheme(ctx)
            } else {
                darkColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Menu.route) {
        composable(Screen.Menu.route) { MenuScreen(navController) }
        composable(
            route = Screen.Diary.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateStr = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
            val date = LocalDate.parse(dateStr)
            DiaryScreen(navController, date)
        }
        composable(Screen.Calendar.route) { CalendarScreen(navController) }
        composable(Screen.UsageStats.route) { UsageStatsScreen(navController) }
    }
}

// --- SCREENS --- //

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MenuScreen(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Diary Four",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(32.dp))

            // Simple cards for navigation
            MenuCard("Diary Entry", Icons.AutoMirrored.Filled.MenuBook) {
                navController.navigate(Screen.Diary.createRoute(LocalDate.now()))
            }
            MenuCard("Calendar View", Icons.Default.CalendarMonth) {
                navController.navigate(Screen.Calendar.route)
            }
            MenuCard("Phone Usage", Icons.Default.Smartphone) {
                navController.navigate(Screen.UsageStats.route)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(navController: NavHostController, initialDate: LocalDate) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // UI state
    var selectedDate by remember { mutableStateOf(initialDate) }
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    var entries by remember { mutableStateOf(listOf<String>()) }
    var message by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Load entries whenever the date changes
    LaunchedEffect(selectedDate) {
        entries = readDiaryEntries(context.filesDir, "${selectedDate}.txt")
    }

    // Small ephemeral message — disappears after 2s
    LaunchedEffect(message) {
        if (message != null) {
            delay(2000)
            message = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diary Entry for $selectedDate") },
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
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Button(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                Spacer(Modifier.width(8.dp))
                Text(selectedDate.toString())
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true // newest entries at the bottom (chat-like)
            ) {
                if (entries.isEmpty()) {
                    item {
                        Text(
                            "No entries for this date.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(entries) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(entry.trim(), modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = message != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = message ?: "",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Input area
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    label = { Text("Write your entry...") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp, max = 160.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FilledTonalButton(onClick = {
                        scope.launch {
                            // clear and update UI
                            clearDiaryFile(context.filesDir, "${selectedDate}.txt")
                            entries = emptyList()
                            message = "Diary cleared"
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear All")
                        Spacer(Modifier.width(8.dp))
                        Text("Clear All")
                    }

                    Button(
                        onClick = {
                            val content = textState.text.trim()
                            if (content.isNotEmpty()) {
                                val entry = "${timestampNow()} — $content\n\n"
                                scope.launch {
                                    if (appendDiaryEntry(context.filesDir, "${selectedDate}.txt", entry)) {
                                        entries = readDiaryEntries(context.filesDir, "${selectedDate}.txt")
                                        textState = TextFieldValue("") // clear input
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

            // Date picker dialog
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

/**
 * Calendar screen — pick a date and view entries for that date.
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

            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedDate == null) {
                    item {
                        Text("Please select a date to view entries.", modifier = Modifier.padding(top = 24.dp), textAlign = TextAlign.Center)
                    }
                } else if (entries.isEmpty()) {
                    item {
                        Text("No entries for this date.", modifier = Modifier.padding(top = 24.dp), textAlign = TextAlign.Center)
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

/**
 * Usage statistics screen — shows daily/weekly totals pulled from UsageStatsManager.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatsScreen(navController: NavHostController) {
    val context = LocalContext.current
    var usageStats by remember { mutableStateOf<Map<LocalDate, Long>>(emptyMap()) }
    var granted by remember { mutableStateOf(hasUsageStatsPermission(context)) }

    // Launcher used to open the system settings for usage access.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        granted = hasUsageStatsPermission(context)
    }

    LaunchedEffect(granted) {
        if (granted) {
            val today = LocalDate.now()
            val usage = getPhoneUsageForDate(context, today)
            savePhoneUsage(context, today, usage)

            // Read last 7 days from storage (0 = today)
            val pastWeekStats = (0..6).map { i ->
                val date = today.minusDays(i.toLong())
                val dailyUsage = readPhoneUsageForDate(context, date)
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
                if (usageStats.isEmpty()) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Calculating usage stats...")
                } else {
                    val weeklyTotal = usageStats.values.sum()

                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total This Week", style = MaterialTheme.typography.titleLarge)
                            Text("$weeklyTotal minutes", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(usageStats.keys.sortedDescending().toList()) { date ->
                            val usage = usageStats[date] ?: 0
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(date.toString(), style = MaterialTheme.typography.bodyLarge)
                                Text("$usage minutes", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            } else {
                // Show a short explanation and a button to open usage access settings.
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
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

// --- UI COMPONENTS --- //

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuCard(text: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.titleLarge)
        }
    }
}

// --- DIARY HELPERS --- //

/**
 * Read diary entries for `filename` from the app files directory.
 * Returns a list of entries (newest first). Runs on IO dispatcher.
 */
suspend fun readDiaryEntries(filesDir: File, filename: String): List<String> = withContext(Dispatchers.IO) {
    try {
        val file = File(filesDir, filename)
        if (!file.exists()) return@withContext emptyList()
        FileInputStream(file).use { fis ->
            val content = fis.readBytes().toString(Charset.defaultCharset())
            // We split by double newline and reverse so newest entries appear last in the list UI.
            content.split("\n\n").filter { it.isNotBlank() }.map { it.trim() }.reversed()
        }
    } catch (e: Exception) {
        // If anything fails, return an empty list — UI will show "No entries".
        emptyList()
    }
}

/**
 * Append a text entry to the diary file.
 * Returns true on success. Runs on IO dispatcher.
 */
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

/**
 * Clear a diary file's contents (overwrite with empty string).
 * Returns true on success. Runs ogdn IO dispatcher.
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
 * Return a human-friendly timestamp (YYYY-MM-DD HH:mm).
 */
fun timestampNow(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = now.time.hour.toString().padStart(2, '0')
    val minute = now.time.minute.toString().padStart(2, '0')
    return "${now.date} $hour:$minute"
}

// --- USAGE STATS HELPERS --- //

/**
 * Check whether the app has permission to read usage stats.
 * Uses the AppOpsManager check appropriate for the platform version.
 */
private fun hasUsageStatsPermission(context: Context): Boolean {
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

/**
 * Compute total phone ussage (in minutes) for a given LocalDate.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun getPhoneUsageForDate(context: Context, date: LocalDate): Long {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)

    val total = stats?.sumOf { it.totalTimeInForeground } ?: 0L
    return TimeUnit.MILLISECONDS.toMinutes(total)
}

/**
 * Persist the usage number for `date` to a ssimple file under filesDir/usage_stats.
 * Any IO errors are swallowed — this is non-critical caching.
 */
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

/**
 * Read the cached usage value for `date`, if present.
 * Returns null if not prresent or parse fails.
 */
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
