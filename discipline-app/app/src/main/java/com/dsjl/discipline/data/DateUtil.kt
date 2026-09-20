package com.dsjl.discipline.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtil {
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayFmt = SimpleDateFormat("M/d", Locale.US)
    private val monthDayFmt = SimpleDateFormat("M月d日", Locale.US)
    private val timeFmt = SimpleDateFormat("M月d日 HH:mm", Locale.US)
    private val weekNames = listOf("日", "一", "二", "三", "四", "五", "六")

    /** 今天的日期字符串 yyyy-MM-dd */
    fun today(): String = fmt.format(Date())

    /** n 天前的日期字符串 */
    fun daysAgo(n: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -n)
        return fmt.format(c.time)
    }

    fun parse(date: String): Date = fmt.parse(date)!!

    fun formatDate(d: Date): String = fmt.format(d)

    /** 例：6月12日 星期五 */
    fun todayLabel(): String {
        val c = Calendar.getInstance()
        val week = weekNames[(c.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
        return "${c.get(Calendar.MONTH) + 1}月${c.get(Calendar.DAY_OF_MONTH)}日 星期$week"
    }

    /** 例：6/12 */
    fun dayLabel(date: String): String = dayFmt.format(parse(date))

    /** 例：6月12日 */
    fun monthDayLabel(date: String): String = monthDayFmt.format(parse(date))

    /** 例：6月12日 09:30 */
    fun timeLabel(epochMillis: Long): String = timeFmt.format(Date(epochMillis))
}
