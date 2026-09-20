package com.dsjl.discipline.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dsjl.discipline.work.WeeklyReportWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 每天 09:00 的闹钟；只有周一才把周报任务交给 WorkManager
 * （WorkManager 负责网络约束与失败重试，广播里不做网络请求）。
 */
class WeeklyReportAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cal = Calendar.getInstance()
        if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = OneTimeWorkRequestBuilder<WeeklyReportWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("weekly_report", ExistingWorkPolicy.REPLACE, work)
    }
}
