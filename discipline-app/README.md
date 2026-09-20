# 自律伙伴（Discipline）

一个帮助你提升自律的安卓应用：每日获取 AI 建议 + 任务清单打卡 + 连续天数与完成率统计 + 每日提醒通知。

## 功能

| 功能 | 说明 |
| --- | --- |
| 每日 AI 建议 | 每天基于你昨天的完成率，让 AI 生成一条个性化的当日建议。未配置 API 或离线时，自动使用内置的 28 条离线自律提醒（每天一条，当天稳定）。 |
| AI 周报 | 每周一 09:00 自动汇总上周 7 天数据（逐日完成率、最佳/最弱一天、各任务完成天数、连续打卡），用 AI 写复盘并推送通知；也可以在「统计」页手动一键生成。未配置 API 时用离线模板周报。 |
| 任务清单 | 添加/编辑/删除任务，支持分类（学习/工作/运动/健康/生活）、优先级（低/中/高）、备注、"每日重复"与"一次性"两种模式。 |
| 连续打卡 | 当天全部完成 = 达成；无任务的中性日不打断连续；有任务没做完则中断。显示当前 🔥 连续天数与最长连续天数。 |
| 统计 | 近 14 天完成情况柱状图、近 7 天完成率、近 30 天完成项数。 |
| 每日提醒 | 每天固定时间推送本地通知（不精确闹钟，省电），开机后自动恢复。 |
| 数据隐私 | 所有数据仅保存在本机 Room 数据库，不上传。 |

## 构建

### 方式一：Android Studio（推荐）

1. 安装 [Android Studio](https://developer.android.com/studio)（Hedgehog 及以上）。
2. `File → Open` 选择本目录（`discipline-app`）。
3. 等待 Gradle Sync 完成（首次会下载 Gradle 8.7 与依赖，需要网络）。
4. 连接手机（开启开发者选项 + USB 调试）或创建模拟器，点 Run 即可。

### 方式二：命令行

需要 JDK 17 与 Android SDK：

```bash
./gradlew assembleDebug        # Linux / macOS
gradlew.bat assembleDebug      # Windows
```

APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`。

## 配置 AI 建议

在 App 内「设置 → 每日 AI 建议」：

- **接口地址**：任意 OpenAI 兼容的 Base URL，默认 `https://api.deepseek.com/v1`
- **API Key**：你的密钥（只存在本机）
- **模型名**：默认 `deepseek-chat`

支持 DeepSeek、OpenAI、Moonshot、智谱、本地 Ollama（`http://<局域网IP>:11434/v1`）等任何兼容 `/chat/completions` 的服务。点「测试连接」验证，点「保存并生效」立即重新生成今日建议。

## 项目结构

```
app/src/main/java/com/dsjl/discipline/
├── DisciplineApp.kt              # Application：通知渠道、启动时恢复闹钟
├── MainActivity.kt               # 入口 + 底部导航（今日/统计/设置）
├── MainViewModel.kt              # 核心业务：任务、打卡、连续统计、AI 建议
├── data/
│   ├── AppDatabase.kt            # Room 实体与 DAO（tasks / completions / day_stats）
│   ├── AiClient.kt               # OpenAI 兼容 chat/completions 客户端
│   ├── AdviceStore.kt            # 离线内置自律提醒
│   ├── DateUtil.kt               # 日期工具
│   ├── Prefs.kt                  # SharedPreferences 封装
│   ├── Reminders.kt              # AlarmManager 每日提醒 + 周报闹钟
│   ├── Streaks.kt                # 连续打卡计算
│   └── WeeklyReportBuilder.kt    # 周报数据汇总 + AI prompt + 离线模板
├── reminder/
│   ├── DailyReminderReceiver.kt  # 提醒通知
│   ├── BootReceiver.kt           # 开机恢复闹钟
│   └── WeeklyReportAlarmReceiver.kt  # 周一触发 → WorkManager
├── work/
│   └── WeeklyReportWorker.kt     # 生成周报：汇总 → AI/离线 → 入库 → 通知
└── ui/
    ├── TodayScreen.kt            # 今日：进度 + AI 建议 + 任务清单
    ├── StatsScreen.kt            # 统计：连续天数 + 柱状图
    ├── SettingsScreen.kt         # 设置：提醒时间 + AI API 配置
    └── theme/Theme.kt            # 主题（支持深色模式）
```

## 技术栈

Kotlin 1.9 · Jetpack Compose (Material 3) · Room · 协程/Flow · WorkManager · minSdk 26 · targetSdk 34

## 后续可以做的（Roadmap）

- [ ] **专注模式 / 番茄钟**：25 分钟计时 + 专注时长统计
- [ ] **习惯热力图**：GitHub 风格的年度打卡热力图
- [ ] **小组件**：桌面快捷打卡
- [ ] **数据导出/备份**：导出 JSON，防止换机丢数据
- [ ] **任务子项与周期**（每天/工作日/自定义间隔）
- [ ] **成就徽章与里程碑通知**（连续 7/30/100 天）
- [ ] **签名与上架**：配置签名密钥后 `./gradlew assembleRelease` 发布到应用商店
- [ ] **应用图标美化**：目前为简单的对勾+火焰矢量图标，可替换设计稿
