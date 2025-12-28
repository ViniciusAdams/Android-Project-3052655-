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
 * primary navigation hub of the application.
 * composable function renders the main menu, providing access to all major feature screens.
 * layout is centered using a Box, which is an efficient choice for this type of simple, constrained UI.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MenuScreen(navController: NavHostController) {
    // Box is used here to achieve simple centering of its child content.
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // A Column to arrange the menu items in a vertical list.
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // The main application title.
            Text(
                "Diary Four",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(32.dp))

            // Navigation is handled by invoking the NavController with the appropriate route.
            // A reusable MenuCard composable is used to maintain a consistent look and feel.
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
 * A private, reusable composable for a single menu item.
 * Encapsulating the UI for a menu item in this way improves maintainability and code clarity.
 *
 * @param text The text label for the menu item.
 * @param icon The icon to be displayed next to the text.
 * @param onClick The navigation action to be performed when the card is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuCard(text: String, icon: ImageVector, onClick: () -> Unit) {
    // Using a Card provides a distinct, elevated surface for each interactive element.
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // A Row is used for the simple horizontal arrangement of the icon and text.
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
