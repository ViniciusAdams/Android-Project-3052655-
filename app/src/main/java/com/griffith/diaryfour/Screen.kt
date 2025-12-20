package com.griffith.diaryfour

import java.time.LocalDate


sealed class Screen(val route: String) {
    object Menu : Screen("menu")
    object Diary : Screen("diary/{date}") {
        fun createRoute(date: LocalDate): String = "diary/$date"
    }
    object Calendar : Screen("calendar")
    object UsageStats : Screen("usage_stats")
    object SensorData : Screen("sensor_data")
}