package com.griffith.diaryfour.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

/**
 * This screen displays real-time data from the device's ambient light sensor.
 * It serves as a demonstration of integrating Android's sensor framework with Jetpack Compose.
 * The implementation emphasizes efficient resource management by tying the sensor listener's lifecycle
 * to the composable's lifecycle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(navController: NavHostController) {
    // Retrieve the Android context and SensorManager.
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    // Get a reference to the default light sensor. This will be null if the device doesn't have one.
    val lightSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    // state variable holds the current light level value from the sensor.
    // nullable because a value may not be immediately available.
    var lightLevel by remember { mutableStateOf<Float?>(null) }

    // `DisposableEffect` is crucial for managing the lifecycle of the sensor listener.
    // ensures that the listener is registered only when the composable is active and unregistered when it's not,
    // essential for preventing battery drain.
    DisposableEffect(lightSensor) {
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                // Update the state with the new sensor value.
                if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                    lightLevel = event.values[0]
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                //callback is not relevant for this particular use case.
            }
        }

        // Register the listener when the effect is first launched.
        lightSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        // The `onDispose` block is called when the composable is removed from the composition.
        // where the listener is unregistered to free up resources.
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    // A standard Scaffold layout.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensor Data") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Handle the case where a device may not have a light sensor.
            if (lightSensor == null) {
                Text("This device does not have an ambient light sensor.")
            } else {
                // Display the current sensor reading.
                Text(
                    "Ambient Light Level:",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = lightLevel?.let { "%.2f lx".format(it) } ?: "Waiting for data...",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
