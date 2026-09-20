package com.dsjl.discipline.data

import android.content.Context

/** SharedPreferences 包装：API 配置、提醒设置、每日重置标记、AI 建议缓存。 */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("discipline_prefs", Context.MODE_PRIVATE)

    var apiBaseUrl: String
        get() = sp.getString("api_base_url", "https://api.deepseek.com/v1") ?: "https://api.deepseek.com/v1"
        set(value) = sp.edit().putString("api_base_url", value).apply()

    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(value) = sp.edit().putString("api_key", value).apply()

    var apiModel: String
        get() = sp.getString("api_model", "deepseek-chat") ?: "deepseek-chat"
        set(value) = sp.edit().putString("api_model", value).apply()

    var reminderEnabled: Boolean
        get() = sp.getBoolean("reminder_enabled", false)
        set(value) = sp.edit().putBoolean("reminder_enabled", value).apply()

    var reminderTime: String
        get() = sp.getString("reminder_time", "09:00") ?: "09:00"
        set(value) = sp.edit().putString("reminder_time", value).apply()

    var weeklyReportEnabled: Boolean
        get() = sp.getBoolean("weekly_report_enabled", true)
        set(value) = sp.edit().putBoolean("weekly_report_enabled", value).apply()

    /** 上次执行每日重置的日期，用于跨天重置任务 */
    var lastResetDate: String
        get() = sp.getString("last_reset_date", "") ?: ""
        set(value) = sp.edit().putString("last_reset_date", value).apply()

    /** 今日 AI 建议缓存（AI 建议每天只请求一次） */
    var adviceDate: String
        get() = sp.getString("advice_date", "") ?: ""
        set(value) = sp.edit().putString("advice_date", value).apply()

    var adviceText: String
        get() = sp.getString("advice_text", "") ?: ""
        set(value) = sp.edit().putString("advice_text", value).apply()

    var adviceFromAi: Boolean
        get() = sp.getBoolean("advice_from_ai", false)
        set(value) = sp.edit().putBoolean("advice_from_ai", value).apply()
}
