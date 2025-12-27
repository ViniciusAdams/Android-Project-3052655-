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
 * This screen is a fun one. It shows the current ambient light level, as detected by the phone's sensor.
 * It's a good example of how to use Android's sensor framework with Jetpack Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(navController: NavHostController) {
    // First, we need to get a hold of the Android sensor manager.
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    // Then, we get the specific sensor we want to use. In this case, it's the light sensor.
    val lightSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    // This is where we'll store the current light level. It's a nullable float because we might not have a value yet.
    var lightLevel by remember { mutableStateOf<Float?>(null) }

    // This is the magic part. DisposableEffect is a great way to manage resources that need to be cleaned up.
    // In our case, we need to make sure we unregister our sensor listener when the screen is no longer visible.
    DisposableEffect(lightSensor) {
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                // When the sensor gives us a new value, we update our state.
                if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                    lightLevel = event.values[0]
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // We don't really care about this for our example, so we'll just leave it empty.
            }
        }

        // We register our listener when the composable is first drawn.
        lightSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        // And we unregister it when the composable is removed from the screen. This is super important to avoid battery drain!
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    // A classic Scaffold layout to give our screen some structure.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensor Data") },
                navigationIcon = {
                    // The good old back button.
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
            // Not all devices have a light sensor, so we need to handle that case.
            if (lightSensor == null) {
                Text("Sorry, this device doesn't seem to have a light sensor.")
            } else {
                // If we have a sensor, we show the current light level.
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
