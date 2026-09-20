package com.dsjl.discipline.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 连续打卡计算。
 * 规则：某天"达成" = 全部每日任务完成（total>0 且 completed>=total）；
 * 某天无任务（中性）不打断连续；有任务但未全部完成会中断当前连续。
 */
object Streaks {

    /** @return (当前连续天数, 最长连续天数) */
    fun calc(stats: List<DayStatEntity>): Pair<Int, Int> {
        val byDate = stats.associate { it.date to it }
        fun complete(s: DayStatEntity?) = s != null && s.totalTasks > 0 && s.completedTasks >= s.totalTasks
        fun neutral(s: DayStatEntity?) = s == null || s.totalTasks == 0

        var current = 0
        var offset = 0
        while (offset <= 3660) {
            val s = byDate[DateUtil.daysAgo(offset)]
            when {
                complete(s) -> {
                    current++
                    offset++
                }
                neutral(s) -> offset++
                else -> break
            }
        }

        // 最长：从最早记录扫到今天
        val minDate = stats.minByOrNull { it.date }?.date
        if (minDate == null) return current to current
        val today = DateUtil.today()
        val cal = Calendar.getInstance()
        cal.time = DateUtil.parse(minDate)
        var run = 0
        var longest = 0
        while (true) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            val s = byDate[dateStr]
            when {
                complete(s) -> run++
                neutral(s) -> { /* 不断连续 */ }
                else -> run = 0
            }
            if (run > longest) longest = run
            if (dateStr >= today) break
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return current to maxOf(longest, current)
    }
}
