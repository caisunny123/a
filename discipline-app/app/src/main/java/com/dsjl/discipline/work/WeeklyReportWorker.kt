package com.dsjl.discipline.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dsjl.discipline.MainActivity
import com.dsjl.discipline.R
import com.dsjl.discipline.data.AiClient
import com.dsjl.discipline.data.AppDatabase
import com.dsjl.discipline.data.Prefs
import com.dsjl.discipline.data.WeeklyReportBuilder
import com.dsjl.discipline.data.WeeklyReportEntity
import com.dsjl.discipline.reminder.Channel

/**
 * 生成近 7 天自律周报：
 * 1. 汇总数据 → 2. 有 API 则 AI 复盘（失败回退离线模板）→ 3. 入库 → 4. 推送通知。
 */
class WeeklyReportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val db = AppDatabase.get(context)
            val prefs = Prefs(context)

            val data = WeeklyReportBuilder.build(db)
            val useAi = prefs.apiBaseUrl.isNotBlank() && prefs.apiKey.isNotBlank()
            val ai = if (useAi) {
                AiClient.chatOnce(
                    prefs.apiBaseUrl,
                    prefs.apiKey,
                    prefs.apiModel,
                    WeeklyReportBuilder.WEEKLY_SYSTEM,
                    WeeklyReportBuilder.toPrompt(data),
                    500,
                    0.7
                )
            } else {
                null
            }
            val content = ai ?: WeeklyReportBuilder.offlineReport(data)

            db.weekReportDao().deleteByWeekStart(data.rangeStart)
            db.weekReportDao().insert(
                WeeklyReportEntity(
                    weekStart = data.rangeStart,
                    weekEnd = data.rangeEnd,
                    generatedAt = System.currentTimeMillis(),
                    content = content,
                    fromAi = ai != null
                )
            )
            notify(context, content)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun notify(context: Context, content: String) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, Channel.ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📊 本周自律周报已生成")
            .setContentText(content.lineSequence().firstOrNull()?.take(60) ?: "点开查看近 7 天的复盘")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(content.take(400))
            )
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setVibrate(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1002, notification)
        } catch (e: SecurityException) {
            // 未授予通知权限时忽略
        }
    }
}
