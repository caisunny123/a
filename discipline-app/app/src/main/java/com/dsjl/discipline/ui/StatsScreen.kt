package com.dsjl.discipline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import com.dsjl.discipline.data.DateUtil
import com.dsjl.discipline.data.WeeklyReportEntity
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsjl.discipline.MainViewModel

@Composable
fun StatsScreen(vm: MainViewModel) {
    val ui by vm.stats.collectAsState()
    val rep by vm.report.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "自律统计",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            ReportCard(
                report = rep.report,
                generating = rep.generating,
                onGenerate = { vm.generateReport() }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox(
                    value = "${ui.currentStreak}",
                    unit = "天",
                    label = "当前连续",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    value = "${ui.longestStreak}",
                    unit = "天",
                    label = "最长连续",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox(
                    value = "${ui.weekRate}",
                    unit = "%",
                    label = "近7天完成率",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    value = "${ui.monthCompleted}",
                    unit = "项",
                    label = "近30天完成",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "最近 14 天完成情况",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    BarChart(daily = ui.daily)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "绿色 = 全部完成，蓝色 = 部分完成，灰色 = 无任务",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "连续打卡规则：\n• 当天所有每日任务完成 = 达成\n• 当天没有任务 = 中性，不打断连续\n• 有任务但没做完 = 连续中断",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: WeeklyReportEntity?,
    generating: Boolean,
    onGenerate: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📊 本周自律周报",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (generating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (report == null) {
                Text(
                    text = "还没有周报。点下方按钮生成近 7 天的复盘（配置了 AI 则自动用 AI 写，否则用离线模板）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = report.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "生成于 ${DateUtil.timeLabel(report.generatedAt)} · " +
                        "${if (report.fromAi) "AI 复盘" else "离线模板"} · " +
                        "覆盖 ${DateUtil.monthDayLabel(report.weekStart)} ~ ${DateUtil.monthDayLabel(report.weekEnd)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onGenerate, enabled = !generating) {
                Text(
                    text = when {
                        generating -> "生成中…"
                        report == null -> "生成周报"
                        else -> "重新生成周报"
                    }
                )
            }
        }
    }
}

@Composable
private fun StatBox(value: String, unit: String, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BarChart(daily: List<MainViewModel.DayBar>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val n = daily.size
        if (n == 0) return@Canvas
        val slot = size.width / n
        val barW = slot * 0.55f
        daily.forEachIndexed { i, bar ->
            val color = when {
                bar.ratio >= 1f -> Color(0xFF22C55E)
                bar.ratio > 0f -> if (bar.isToday) Color(0xFF3B82F6) else Color(0xFF93C5FD)
                else -> Color(0xFFE2E8F0)
            }
            val h = (bar.ratio * size.height * 0.88f).coerceAtLeast(4.dp.toPx())
            val x = i * slot + (slot - barW) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        daily.forEachIndexed { i, bar ->
            Text(
                text = if (i % 2 == 0) bar.label else "",
                fontSize = 9.sp,
                color = if (bar.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
