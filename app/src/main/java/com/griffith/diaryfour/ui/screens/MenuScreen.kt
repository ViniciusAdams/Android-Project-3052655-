package com.griffith.diaryfour.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.griffith.diaryfour.Screen
import java.time.LocalDate

/**
 * This is our app's main menu. It's a simple screen with a few buttons to get to the other screens.
 * We're using a Box to center the content, which is a nice and easy way to do that.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MenuScreen(navController: NavHostController) {
    // A Box to center the menu on the screen.
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // A Column to lay out the menu items vertically.
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // The app's title. It's big and bold, just like we like it.
            Text(
                "Diary Four",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(32.dp))

            // These are the buttons that take you to the other screens.
            // We're using a reusable `MenuCard` composable to keep the code clean.
            MenuCard("Diary Entry", Icons.AutoMirrored.Filled.MenuBook) {
                navController.navigate(Screen.Diary.createRoute(LocalDate.now()))
            }
            MenuCard("Calendar View", Icons.Default.CalendarMonth) {
                navController.navigate(Screen.Calendar.route)
            }
            MenuCard("Phone Usage", Icons.Default.Smartphone) {
                navController.navigate(Screen.UsageStats.route)
            }
            MenuCard("Sensor Data", Icons.Default.WbSunny) {
                navController.navigate(Screen.SensorData.route)
            }
        }
    }
}

/**
 * This is a reusable composable that we use to create the menu items.
 * It's a simple Card with an icon and some text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuCard(text: String, icon: ImageVector, onClick: () -> Unit) {
    // A Card is a nice way to group content together.
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // A Row to lay out the icon and text horizontally.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.titleLarge)
        }
    }
}
