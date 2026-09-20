package com.dsjl.discipline

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.dsjl.discipline.data.Prefs
import com.dsjl.discipline.data.Reminders
import com.dsjl.discipline.reminder.Channel

class DisciplineApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Channel.ID,
                "每日提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "每天固定时间提醒完成今日任务"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        // 进程启动时确保闹钟在位（覆盖被系统清理/改时间等场景）
        val prefs = Prefs(this)
        if (prefs.reminderEnabled) {
            Reminders.schedule(this, prefs.reminderTime)
        }
        if (prefs.weeklyReportEnabled) {
            Reminders.scheduleWeeklyReport(this)
        }
    }
}
