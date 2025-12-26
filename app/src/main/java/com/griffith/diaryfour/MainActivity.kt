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

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            DiaryfourTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
    // By starting the app at the Menu, we ensure the back button works everywhere else.
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
        composable(Screen.SensorData.route) { SensorScreen(navController) }
    }
}