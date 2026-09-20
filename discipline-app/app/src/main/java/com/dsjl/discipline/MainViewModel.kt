package com.dsjl.discipline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dsjl.discipline.data.AdviceStore
import com.dsjl.discipline.data.AiClient
import com.dsjl.discipline.data.AppDatabase
import com.dsjl.discipline.data.CompletionEntity
import com.dsjl.discipline.data.DateUtil
import com.dsjl.discipline.data.DayStatEntity
import com.dsjl.discipline.data.Prefs
import com.dsjl.discipline.data.Reminders
import com.dsjl.discipline.data.Streaks
import com.dsjl.discipline.data.TaskEntity
import com.dsjl.discipline.data.WeeklyReportBuilder
import com.dsjl.discipline.data.WeeklyReportEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    data class TodayUi(
        val dateLabel: String = DateUtil.todayLabel(),
        val tasks: List<TaskEntity> = emptyList(),
        val completedCount: Int = 0,
        val currentStreak: Int = 0,
        val advice: String? = null,
        val adviceLoading: Boolean = false,
        val adviceFromAi: Boolean = false
    )

    data class DayBar(
        val label: String,
        val ratio: Float,
        val hasTasks: Boolean,
        val isToday: Boolean
    )

    data class StatsUi(
        val daily: List<DayBar> = emptyList(),
        val currentStreak: Int = 0,
        val longestStreak: Int = 0,
        val weekRate: Int = 0,
        val monthCompleted: Int = 0
    )

    data class ReportUi(
        val report: WeeklyReportEntity? = null,
        val generating: Boolean = false
    )

    data class SettingsUi(
        val baseUrl: String,
        val apiKey: String,
        val model: String,
        val reminderOn: Boolean,
        val reminderTime: String,
        val weeklyOn: Boolean = false,
        val message: String? = null
    )

    private val db = AppDatabase.get(application)
    private val prefs = Prefs(application)

    private val _today = MutableStateFlow(TodayUi())
    val today: StateFlow<TodayUi> = _today.asStateFlow()

    private val _stats = MutableStateFlow(StatsUi())
    val stats: StateFlow<StatsUi> = _stats.asStateFlow()

    private val _settings = MutableStateFlow(
        SettingsUi(
            baseUrl = prefs.apiBaseUrl,
            apiKey = prefs.apiKey,
            model = prefs.apiModel,
            reminderOn = prefs.reminderEnabled,
            reminderTime = prefs.reminderTime,
            weeklyOn = prefs.weeklyReportEnabled
        )
    )
    val settings: StateFlow<SettingsUi> = _settings.asStateFlow()

    private val _report = MutableStateFlow(ReportUi())
    val report: StateFlow<ReportUi> = _report.asStateFlow()

    init {
        viewModelScope.launch {
            db.taskDao().observeAll().collect { tasks ->
                _today.update {
                    it.copy(
                        tasks = sortTasks(tasks),
                        completedCount = tasks.count { t -> t.completedAt != null }
                    )
                }
            }
        }
        viewModelScope.launch {
            db.dayStatDao().observeAll().collect { statsList ->
                val (current, longest) = Streaks.calc(statsList)
                _today.update { it.copy(currentStreak = current) }
                _stats.update { buildStats(statsList, current, longest) }
            }
        }
        viewModelScope.launch {
            db.weekReportDao().observeLatest(1).collect { list ->
                _report.update { it.copy(report = list.firstOrNull()) }
            }
        }
        viewModelScope.launch {
            checkDailyReset()
            ensureTodayStat()
            loadAdvice()
        }
    }

    // ---------- 任务操作 ----------

    private fun sortTasks(tasks: List<TaskEntity>): List<TaskEntity> =
        tasks.sortedWith(compareBy({ it.completedAt != null }, { -it.priority }, { it.createdAt }))

    fun addTask(title: String, note: String, category: String, priority: Int, daily: Boolean) {
        viewModelScope.launch {
            db.taskDao().insert(
                TaskEntity(
                    title = title,
                    note = note,
                    category = category,
                    priority = priority,
                    dailyRecurring = daily
                )
            )
            ensureTodayStat()
        }
    }

    fun updateTask(task: TaskEntity, title: String, note: String, category: String, priority: Int, daily: Boolean) {
        viewModelScope.launch {
            db.taskDao().update(task.copy(title = title, note = note, category = category, priority = priority, dailyRecurring = daily))
            ensureTodayStat()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            db.completionDao().deleteByTask(task.id)
            db.taskDao().delete(task)
            ensureTodayStat()
        }
    }

    fun toggleTask(task: TaskEntity) {
        val date = DateUtil.today()
        viewModelScope.launch {
            if (task.completedAt == null) {
                db.taskDao().update(task.copy(completedAt = System.currentTimeMillis()))
                db.completionDao().insert(
                    CompletionEntity(taskId = task.id, date = date, completedAt = System.currentTimeMillis())
                )
            } else {
                db.taskDao().update(task.copy(completedAt = null))
                db.completionDao().delete(task.id, date)
            }
            ensureTodayStat()
        }
    }

    // ---------- 每日重置与统计 ----------

    /** 跨天检查：新的一天开始时重置所有每日重复任务的完成状态。 */
    private suspend fun checkDailyReset() {
        val today = DateUtil.today()
        if (prefs.lastResetDate == today) return
        val tasks = db.taskDao().getAllOnce()
        for (t in tasks) {
            if (t.dailyRecurring && t.completedAt != null) {
                db.taskDao().update(t.copy(completedAt = null))
            }
        }
        prefs.lastResetDate = today
    }

    /** 按当前任务列表刷新今日快照（total/completed 以每日重复任务计）。 */
    private suspend fun ensureTodayStat() {
        val tasks = db.taskDao().getAllOnce()
        val total = tasks.count { it.dailyRecurring }
        val completed = tasks.count { it.dailyRecurring && it.completedAt != null }
        db.dayStatDao().upsert(DayStatEntity(DateUtil.today(), total, completed))
    }

    private fun buildStats(stats: List<DayStatEntity>, current: Int, longest: Int): StatsUi {
        val byDate = stats.associate { it.date to it }
        val daily = (13 downTo 0).map { n ->
            val d = DateUtil.daysAgo(n)
            val s = byDate[d]
            val total = s?.totalTasks ?: 0
            val comp = s?.completedTasks ?: 0
            DayBar(
                label = DateUtil.dayLabel(d),
                ratio = if (total > 0) comp.toFloat() / total else 0f,
                hasTasks = total > 0,
                isToday = n == 0
            )
        }
        val week = (6 downTo 0).map { byDate[DateUtil.daysAgo(it)] ?: DayStatEntity("", 0, 0) }
        val weekTotal = week.sumOf { it.totalTasks }
        val weekComp = week.sumOf { it.completedTasks }
        val weekRate = if (weekTotal > 0) weekComp * 100 / weekTotal else 0
        val monthCompleted = stats
            .filter { it.date >= DateUtil.daysAgo(29) }
            .sumOf { it.completedTasks }
        return StatsUi(
            daily = daily,
            currentStreak = current,
            longestStreak = longest,
            weekRate = weekRate,
            monthCompleted = monthCompleted
        )
    }

    // ---------- 每日 AI 建议 ----------

    fun loadAdvice(force: Boolean = false) {
        val date = DateUtil.today()
        if (!force && prefs.adviceDate == date && prefs.adviceText.isNotEmpty()) {
            _today.update {
                it.copy(advice = prefs.adviceText, adviceFromAi = prefs.adviceFromAi)
            }
            return
        }
        _today.update { it.copy(adviceLoading = true) }
        viewModelScope.launch {
            val useAi = prefs.apiBaseUrl.isNotBlank() && prefs.apiKey.isNotBlank()
            val yesterday = DateUtil.daysAgo(1)
            val yStat = db.dayStatDao().get(yesterday)
            val yRate = if (yStat != null && yStat.totalTasks > 0) {
                yStat.completedTasks * 100 / yStat.totalTasks
            } else {
                -1
            }
            val tasks = db.taskDao().getAllOnce()
            val summary = buildString {
                append("今天是 ").append(DateUtil.todayLabel()).append("。")
                if (yRate >= 0) {
                    append("昨天任务完成率 ").append(yRate).append("%。")
                } else {
                    append("昨天没有任务记录。")
                }
                append("今日任务共 ").append(tasks.count { it.dailyRecurring }).append(" 项：")
                append(tasks.joinToString("、") { it.title })
                append("。请给出今日建议。")
            }
            val ai = if (useAi) {
                AiClient.fetchAdvice(prefs.apiBaseUrl, prefs.apiKey, prefs.apiModel, summary)
            } else {
                null
            }
            val fromAi = ai != null
            val text = ai ?: AdviceStore.pick(date)
            if (fromAi) {
                prefs.adviceDate = date
                prefs.adviceText = text
                prefs.adviceFromAi = true
            }
            _today.update {
                it.copy(advice = text, adviceLoading = false, adviceFromAi = fromAi)
            }
        }
    }

    // ---------- 设置 ----------

    fun saveApi(baseUrl: String, key: String, model: String) {
        prefs.apiBaseUrl = baseUrl.trim()
        prefs.apiKey = key.trim()
        prefs.apiModel = model.trim()
        _settings.update {
            it.copy(
                baseUrl = prefs.apiBaseUrl,
                apiKey = prefs.apiKey,
                model = prefs.apiModel,
                message = "已保存，并立即重新生成今日建议"
            )
        }
        loadAdvice(force = true)
    }

    fun testApi(baseUrl: String, key: String, model: String) {
        _settings.update { it.copy(message = "正在测试连接…") }
        viewModelScope.launch {
            val result = AiClient.test(baseUrl.trim(), key.trim(), model.trim())
            _settings.update { it.copy(message = result) }
        }
    }

    fun setReminder(on: Boolean, time: String) {
        prefs.reminderEnabled = on
        prefs.reminderTime = time
        if (on) {
            Reminders.schedule(getApplication(), time)
            _settings.update { it.copy(reminderOn = true, reminderTime = time, message = "✅ 已开启，每天 $time 提醒你") }
        } else {
            Reminders.cancel(getApplication())
            _settings.update { it.copy(reminderOn = false, message = "提醒已关闭") }
        }
    }

    /** 每周一 09:00 自动生成上周周报（WorkManager 执行，带网络约束与重试）。 */
    fun setWeeklyReport(on: Boolean) {
        prefs.weeklyReportEnabled = on
        if (on) {
            Reminders.scheduleWeeklyReport(getApplication())
            _settings.update { it.copy(weeklyOn = true, message = "✅ 每周一 09:00 自动生成周报并推送通知") }
        } else {
            Reminders.cancelWeeklyReport(getApplication())
            _settings.update { it.copy(weeklyOn = false, message = "自动周报已关闭") }
        }
    }

    /** 手动生成近 7 天周报（同一周重复生成会覆盖旧报告）。 */
    fun generateReport() {
        if (_report.value.generating) return
        _report.update { it.copy(generating = true) }
        viewModelScope.launch {
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
            _report.update { it.copy(generating = false) }
        }
    }

    fun consumeSettingsMessage() {
        _settings.update { it.copy(message = null) }
    }
}
