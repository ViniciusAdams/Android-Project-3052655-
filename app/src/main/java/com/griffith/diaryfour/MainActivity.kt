package com.griffith.diaryfour

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.griffith.diaryfour.ui.screens.CalendarScreen
import com.griffith.diaryfour.ui.screens.DiaryScreen
import com.griffith.diaryfour.ui.screens.MenuScreen
import com.griffith.diaryfour.ui.screens.SensorScreen
import com.griffith.diaryfour.ui.screens.UsageStatsScreen
import com.griffith.diaryfour.ui.theme.DiaryfourTheme
import java.time.LocalDate

/**
 * heart of the app, the main activity.
 * Jetpack Compose to build the UI.
 */
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // activity to use our Compose UI.
        setContent {
            //  wrap entire app in our custom theme.
            DiaryfourTheme {
                // A Surface is like a canvas for UI.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // we set up all the navigation between our screens.
                    AppNavigation()
                }
            }
        }
    }
}

/**
 * composable  app's navigation hub. defines all the possible screens
 * and how to get to them.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    // NavController is the engine that drives our navigation.
    val navController = rememberNavController()

    // NavHost is the container for all the screens.
    NavHost(navController = navController, startDestination = Screen.Menu.route) {
        // main menu screen. the first thing the user sees.
        composable(Screen.Menu.route) { MenuScreen(navController) }

        // The diary screen. It takes a date as an argument,  show the entries for a specific day.
        composable(
            route = Screen.Diary.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            // get the date from the navigation arguments, or just use today's date if it's not there.
            val dateStr = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
            val date = LocalDate.parse(dateStr)
            DiaryScreen(navController, date)
        }

        // The rest of the screens. They're all pretty straightforward.
        composable(Screen.Calendar.route) { CalendarScreen(navController) }
        composable(Screen.UsageStats.route) { UsageStatsScreen(navController) }
        composable(Screen.SensorData.route) { SensorScreen(navController) }
    }
}
