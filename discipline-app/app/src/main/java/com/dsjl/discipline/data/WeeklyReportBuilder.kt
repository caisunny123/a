package com.dsjl.discipline.data

import java.util.Calendar
import java.util.Date

/** 近 7 天（含今天）的自律数据汇总。 */
data class WeeklyReportData(
    val rangeLabel: String,
    val rangeStart: String,
    val rangeEnd: String,
    val dayLines: List<String>,
    val overallRate: Int,
    val totalCompleted: Int,
    val totalPossible: Int,
    val currentStreak: Int,
    val bestDay: String?,
    val worstDay: String?,
    val taskLines: List<String>
)

object WeeklyReportBuilder {

    const val WEEKLY_SYSTEM =
        "你是一位温和而坚定的自律教练。用户会给你近7天的任务完成数据，" +
        "请写一份简短（不超过250字）的周度复盘周报：先具体肯定成果（引用真实数字），" +
        "再指出1-2个可改进点，最后给下周一个具体、可执行的小目标。" +
        "语气积极、不说空话、不要列表符号，直接成文。"

    /** 汇总近 7 天（含今天）数据。 */
    suspend fun build(db: AppDatabase): WeeklyReportData {
        val end = DateUtil.today()
        val start = DateUtil.daysAgo(6)

        val stats = db.dayStatDao().range(start, end)
        val allStats = db.dayStatDao().recent(400)
        val (currentStreak, _) = Streaks.calc(allStats)

        val dayLines = (0 until 7).map { i ->
            val d = DateUtil.daysAgo(6 - i)
            val s = stats.firstOrNull { it.date == d }
            val total = s?.totalTasks ?: 0
            val comp = s?.completedTasks ?: 0
            val rate = if (total > 0) comp * 100 / total else -1
            val suffix = if (rate >= 0) " $rate%" else ""
            "${DateUtil.dayLabel(d)}: $comp/$total$suffix"
        }

        val totalPossible = stats.sumOf { it.totalTasks }
        val totalCompleted = stats.sumOf { it.completedTasks }
        val overallRate = if (totalPossible > 0) totalCompleted * 100 / totalPossible else -1

        val withTasks = stats.filter { it.totalTasks > 0 }
        fun line(s: DayStatEntity) = "${DateUtil.dayLabel(s.date)}（${s.completedTasks}/${s.totalTasks}）"
        val bestDay = withTasks.maxByOrNull { it.completedTasks.toFloat() / it.totalTasks }?.let(:line)
        val worstDay = withTasks.minByOrNull { it.completedTasks.toFloat() / it.totalTasks }?.let(:line)

        val completions = db.completionDao().range(start, end)
        val tasks = db.taskDao().getAllOnce().filter { it.dailyRecurring }
        val taskLines = tasks.mapNotNull { t ->
            val possible = possibleDays(t, start, end)
            if (possible <= 0) null else {
                val done = completions.count { it.taskId == t.id }
                "${t.title} $done/$possible"
            }
        }

        return WeeklyReportData(
            rangeLabel = "${DateUtil.monthDayLabel(start)} ~ ${DateUtil.monthDayLabel(end)}",
            rangeStart = start,
            rangeEnd = end,
            dayLines = dayLines,
            overallRate = overallRate,
            totalCompleted = totalCompleted,
            totalPossible = totalPossible,
            currentStreak = currentStreak,
            bestDay = bestDay,
            worstDay = worstDay,
            taskLines = taskLines
        )
    }

    /** 任务在 [start, end] 区间内应完成的天数（受创建日期约束）。 */
    private fun possibleDays(task: TaskEntity, start: String, end: String): Int {
        val cal = Calendar.getInstance()
        cal.time = Date(task.createdAt)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val createdStr = DateUtil.formatDate(cal.time)
        val first = if (createdStr > start) createdStr else start
        if (first > end) return 0
        val c = Calendar.getInstance()
        c.time = DateUtil.parse(first)
        var n = 0
        while (true) {
            val ds = DateUtil.formatDate(c.time)
            n++
            if (ds >= end) break
            c.add(Calendar.DAY_OF_YEAR, 1)
        }
        return n
    }

    /** 把数据拼成给 AI 的用户消息。 */
    fun toPrompt(d: WeeklyReportData): String = buildString {
        append("近7天（").append(d.rangeLabel).append("）我的自律数据：\n")
        if (d.totalPossible > 0) {
            append("逐日完成：\n").append(d.dayLines.joinToString("\n")).append("\n")
            append("整体完成率：").append(d.overallRate)
                .append("%（完成 ").append(d.totalCompleted).append("/").append(d.totalPossible).append(" 项）\n")
            append("当前连续打卡：").append(d.currentStreak).append(" 天\n")
            d.bestDay?.let { append("最佳一天：").append(it).append("\n") }
            d.worstDay?.let { append("最弱一天：").append(it).append("\n") }
            if (d.taskLines.isNotEmpty()) {
                append("各任务完成（完成天数/应完成天数）：\n").append(d.taskLines.joinToString("\n")).append("\n")
            }
        } else {
            append("本周没有任务记录。\n")
        }
        append("请写本周周报。")
    }

    /** 离线模板周报（未配置 API 或请求失败时使用）。 */
    fun offlineReport(d: WeeklyReportData): String = buildString {
        append("📊 本周自律周报（").append(d.rangeLabel).append("）\n")
        if (d.totalPossible == 0) {
            append("本周还没有任务数据。先从每天 3 件小事开始（比如喝水 8 杯 / 运动 30 分钟 / 23 点前睡觉），下周再来复盘。")
            return@buildString
        }
        append("• 整体完成率：").append(d.overallRate).append("%（完成 ")
            .append(d.totalCompleted).append("/").append(d.totalPossible).append(" 项）\n")
        d.bestDay?.let { append("• 最佳一天：").append(it).append("\n") }
        if (d.worstDay != null && d.worstDay != d.bestDay) {
            append("• 最弱一天：").append(d.worstDay).append("\n")
        }
        append("• 当前连续打卡：").append(d.currentStreak).append(" 天\n")
        if (d.taskLines.isNotEmpty()) {
            append("• 各任务：").append(d.taskLines.joinToString(" ｜ ")).append("\n")
        }
        val verdict = when {
            d.overallRate >= 80 -> "总评：本周节奏很稳，把这种状态保持住，下周的目标可以稍微加码。"
            d.overallRate >= 50 -> "总评：本周整体不错。薄弱的一天别自责，状态会波动，关键是第二天能回到节奏里。"
            else -> "总评：本周节奏偏弱，别急着加量。下周只挑最关键的 2 件事，做完再说。"
        }
        append(verdict)
    }
}
