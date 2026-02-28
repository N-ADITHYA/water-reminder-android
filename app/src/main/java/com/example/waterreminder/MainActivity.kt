package com.example.waterreminder

import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import java.util.concurrent.TimeUnit
import androidx.work.WorkManager
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContent {
            WaterReminderApp()
        }
    }
}

@Composable
fun WaterReminderApp() {

    var timeRemaining by remember { mutableStateOf("") }
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
    val nextTriggerTime = sharedPref.getLong("next_trigger_time", 0L)
    val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    val savedDate = sharedPref.getString("saved_date", "")
    var waterCount by remember {
        mutableStateOf(sharedPref.getInt("water_count", 0))
    }
    var isReminderOn by remember {
        mutableStateOf(sharedPref.getBoolean("reminder_status", false))
    }

    LaunchedEffect(isReminderOn) {
        while (isReminderOn) {

            var nextTriggerTime = sharedPref.getLong("next_trigger_time", 0L)
            val intervalMinutes = sharedPref.getLong("interval_minutes", 0L)

            val diff = nextTriggerTime - System.currentTimeMillis()

            if (diff <= 0 && intervalMinutes > 0) {
                nextTriggerTime = System.currentTimeMillis() + intervalMinutes * 60 * 1000

                sharedPref.edit()
                    .putLong("next_trigger_time", nextTriggerTime)
                    .apply()
            }

            val remaining = nextTriggerTime - System.currentTimeMillis()

            if (remaining > 0) {
                val minutes = remaining / 60000
                val seconds = (remaining % 60000) / 1000
                timeRemaining = "Next reminder in ${minutes}m ${seconds}s"
            }

            kotlinx.coroutines.delay(1000)
        }
    }

    if (savedDate != todayDate) {
        waterCount = 0
        sharedPref.edit()
            .putInt("water_count", 0)
            .putString("saved_date", todayDate)
            .apply()
    }
    var interval by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isReminderOn) "Reminder Status: ON 🟢" else "Reminder Status: OFF 🔴",
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(timeRemaining, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "💧 Water Reminder",
            fontSize = 26.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = interval,
            onValueChange = { interval = it },
            label = { Text(
                "Interval(set at least 15 mins)") }

        )


        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val minutes = interval.toLongOrNull() ?: return@Button

            val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
                minutes,
                TimeUnit.MINUTES
            )
                .setInitialDelay(minutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "waterReminder",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )

            val nextTrigger = System.currentTimeMillis() + (minutes * 60 * 1000)

            isReminderOn = true
            sharedPref.edit().putBoolean("reminder_status", true).apply()

            sharedPref.edit()
                .putLong("interval_minutes", minutes)
                .putLong("next_trigger_time", System.currentTimeMillis() + minutes * 60 * 1000)
                .apply()


        }) {
            Text("Start Reminder")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
            WorkManager.getInstance(context).cancelUniqueWork("waterReminder")

            isReminderOn = false
            sharedPref.edit().putBoolean("reminder_status", false).apply()

            timeRemaining = "No active reminder"

        }) {
            Text("Stop Reminder")
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Glasses today: $waterCount",
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
            waterCount++

            sharedPref.edit()
                .putInt("water_count", waterCount)
                .putString("saved_date", todayDate)
                .apply()

        }) {
            Text("I Drank Water")
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
            waterCount = 0

            sharedPref.edit()
                .putInt("water_count", 0)
                .apply()

        }) {
            Text("Reset Counter")
        }

    }


}