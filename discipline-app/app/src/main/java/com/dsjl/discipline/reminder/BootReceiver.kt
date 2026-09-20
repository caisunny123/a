package com.dsjl.discipline.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dsjl.discipline.data.Prefs
import com.dsjl.discipline.data.Reminders

/** 开机后重新注册每日提醒闹钟。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = Prefs(context)
            if (prefs.reminderEnabled) {
                Reminders.schedule(context, prefs.reminderTime)
            }
        }
    }
}
