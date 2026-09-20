package com.dsjl.discipline.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dsjl.discipline.reminder.DailyReminderReceiver
import com.dsjl.discipline.reminder.WeeklyReportAlarmReceiver
import java.util.Calendar

/** 每日固定时间提醒（不精确闹钟，省电）。 */
object Reminders {
    private const val REQUEST_ID = 1001
    private const val WEEKLY_REQUEST_ID = 2001

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun weeklyPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WeeklyReportAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            WEEKLY_REQUEST_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** time 格式 "HH:mm" */
    fun schedule(context: Context, time: String) {
        val parts = time.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return
        if (hour !in 0..23 || minute !in 0..59) return

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setRepeating(AlarmManager.RTC, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent(context))
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }

    /**
     * 每周自动周报：每天 09:00 触发一次闹钟，
     * 由 [com.dsjl.discipline.reminder.WeeklyReportAlarmReceiver] 判断是否周一后交给 WorkManager。
     */
    fun scheduleWeeklyReport(context: Context) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setRepeating(AlarmManager.RTC, cal.timeInMillis, AlarmManager.INTERVAL_DAY, weeklyPendingIntent(context))
    }

    fun cancelWeeklyReport(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(weeklyPendingIntent(context))
    }
}
