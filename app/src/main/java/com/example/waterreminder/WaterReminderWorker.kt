package com.example.waterreminder

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class WaterReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {

        val sharedPref =
            applicationContext.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)

        val intervalMinutes = sharedPref.getLong("interval_minutes", 0L)

        if (intervalMinutes > 0) {
            val nextTrigger =
                System.currentTimeMillis() + intervalMinutes * 60 * 1000

            sharedPref.edit()
                .putLong("next_trigger_time", nextTrigger)
                .apply()
        }

        NotificationHelper().showNotification(applicationContext)

        return Result.success()
    }
}