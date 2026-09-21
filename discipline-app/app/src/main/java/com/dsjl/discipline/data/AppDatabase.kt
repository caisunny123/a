package com.dsjl.discipline.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * 一个任务。dailyRecurring = true 表示每天重复（每天零点重置完成状态），
 * false 表示一次性任务，完成后不再重复。
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val category: String = "默认",
    /** 0=低 1=中 2=高 */
    val priority: Int = 1,
    val dailyRecurring: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    /** 今日完成时间；null 表示未完成（每日重复任务每天重置） */
    val completedAt: Long? = null
)

/** 历史打卡记录：某天完成了某个任务。 */
@Entity(tableName = "completions")
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    /** yyyy-MM-dd */
    val date: String,
    val completedAt: Long
)

/** 每日快照：用于统计与连续打卡计算。 */
@Entity(tableName = "day_stats")
data class DayStatEntity(
    @PrimaryKey val date: String,
    val totalTasks: Int,
    val completedTasks: Int
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY priority DESC, createdAt ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY priority DESC, createdAt ASC")
    suspend fun getAllOnce(): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)
}

/** 每周自律周报（AI 生成或离线模板）。 */
@Entity(tableName = "weekly_reports")
data class WeeklyReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 周区间起始日 yyyy-MM-dd */
    val weekStart: String,
    /** 周区间结束日 yyyy-MM-dd */
    val weekEnd: String,
    val generatedAt: Long,
    val content: String,
    val fromAi: Boolean
)

@Dao
interface CompletionDao {
    @Insert
    suspend fun insert(entity: CompletionEntity): Long

    @Query("DELETE FROM completions WHERE taskId = :taskId AND date = :date")
    suspend fun delete(taskId: Long, date: String)

    @Query("DELETE FROM completions WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: Long)

    @Query("SELECT COUNT(*) FROM completions WHERE date >= :since")
    suspend fun countSince(since: String): Int

    @Query("SELECT * FROM completions WHERE date >= :start AND date <= :end")
    suspend fun range(start: String, end: String): List<CompletionEntity>
}

@Dao
interface DayStatDao {
    @Query("SELECT * FROM day_stats ORDER BY date DESC")
    fun observeAll(): Flow<List<DayStatEntity>>

    @Query("SELECT * FROM day_stats WHERE date = :date")
    suspend fun get(date: String): DayStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DayStatEntity)

    @Query("SELECT * FROM day_stats WHERE date >= :start AND date <= :end ORDER BY date ASC")
    suspend fun range(start: String, end: String): List<DayStatEntity>

    @Query("SELECT * FROM day_stats ORDER BY date DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<DayStatEntity>
}

@Dao
interface WeekReportDao {
    @Query("SELECT * FROM weekly_reports ORDER BY generatedAt DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<WeeklyReportEntity>>

    @Query("DELETE FROM weekly_reports WHERE weekStart = :weekStart")
    suspend fun deleteByWeekStart(weekStart: String)

    @Insert
    suspend fun insert(entity: WeeklyReportEntity): Long
}

@Database(
    entities = [TaskEntity::class, CompletionEntity::class, DayStatEntity::class, WeeklyReportEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun completionDao(): CompletionDao
    abstract fun dayStatDao(): DayStatDao
    abstract fun weekReportDao(): WeekReportDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `weekly_reports` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`weekStart` TEXT NOT NULL, " +
                        "`weekEnd` TEXT NOT NULL, " +
                        "`generatedAt` INTEGER NOT NULL, " +
                        "`content` TEXT NOT NULL, " +
                        "`fromAi` INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "discipline.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
