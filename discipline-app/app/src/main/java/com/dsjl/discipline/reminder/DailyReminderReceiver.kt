package com.dsjl.discipline.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dsjl.discipline.MainActivity
import com.dsjl.discipline.R

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Channel.ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🌅 新的一天，开始自律")
            .setContentText("看看今日 AI 建议和任务清单，逐条完成它们吧！")
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setVibrate(longArrayOf(0L, 300L, 200L, 300L))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (e: SecurityException) {
            // 未授予通知权限时忽略
        }
    }
}
