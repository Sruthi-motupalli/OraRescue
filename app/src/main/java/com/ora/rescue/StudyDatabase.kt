package com.ora.rescue

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Entity representing an educational study topic entered by the student.
 * Maps to the "tasks" table in SQLite.
 *
 * @property id Auto-generated primary key.
 * @property subject Broad academic subject (e.g., "Computer Science").
 * @property topic Specific study topic or chapter (e.g., "Scheduling Algorithms").
 * @property examDay Exam deadline stored as epoch day (days since 1970-01-01).
 * @property totalMinutes Total estimated study time required.
 * @property priority Priority level: 3 (High), 2 (Medium), 1 (Low).
 */
@Entity(tableName = "tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val topic: String,
    val examDay: Long,
    val totalMinutes: Int,
    val priority: Int,
    @ColumnInfo(defaultValue = "NULL") val chapterId: Long? = null
)

@Entity(tableName = "course_materials")
data class CourseMaterial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceUri: String,
    val mimeType: String,
    val importedAt: Long
)

@Entity(
    tableName = "study_chapters",
    foreignKeys = [ForeignKey(
        entity = CourseMaterial::class,
        parentColumns = ["id"],
        childColumns = ["materialId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("materialId")]
)
data class StudyChapter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: Long,
    val title: String,
    val orderIndex: Int
)

@Entity(
    tableName = "quiz_questions",
    foreignKeys = [ForeignKey(
        entity = StudyChapter::class,
        parentColumns = ["id"],
        childColumns = ["chapterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("chapterId")]
)
data class QuizQuestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val prompt: String,
    val options: String,
    val correctIndex: Int,
    val explanation: String,
    val source: String = SOURCE_CURATED
) {
    fun optionList(): List<String> = options.split(OPTION_SEPARATOR)

    companion object {
        const val OPTION_SEPARATOR = "\u001F"
        const val SOURCE_CURATED = "Course material"
        const val SOURCE_STUDENT = "Student question"
        const val SOURCE_AI = "Gemini · course material"
        fun encodeOptions(values: List<String>) = values.joinToString(OPTION_SEPARATOR)
    }
}

@Entity(
    tableName = "quiz_attempts",
    foreignKeys = [ForeignKey(
        entity = StudyChapter::class,
        parentColumns = ["id"],
        childColumns = ["chapterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("chapterId"), Index("sessionId")]
)
data class QuizAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val sessionId: Long?,
    val score: Int,
    val total: Int,
    val completedAt: Long
)

/**
 * Entity representing user-configured available study minutes for a specific calendar day.
 * Retained for backward compatibility and fast summary queries.
 *
 * @property day Calendar day stored as epoch day (Primary Key).
 * @property minutes Available study minutes on this day (0 to 1440).
 */
@Entity(tableName = "availability")
data class DailyAvailability(
    @PrimaryKey val day: Long,
    val minutes: Int
)

/**
 * Entity representing a specific free-time window configured for a calendar day.
 * New in Room Schema Version 2.
 *
 * @property id Auto-generated primary key.
 * @property day Calendar day stored as epoch day.
 * @property startMinute Minute from midnight (0..1439, e.g. 960 = 16:00).
 * @property endMinute Minute from midnight (1..1440, e.g. 1020 = 17:00).
 */
@Entity(
    tableName = "time_windows",
    indices = [Index("day")]
)
data class TimeWindow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val startMinute: Int,
    val endMinute: Int
) {
    /** Duration in minutes of this free-time interval. */
    val durationMinutes: Int get() = (endMinute - startMinute).coerceAtLeast(0)

    /** Human-readable time range string, e.g. "16:00 – 17:00". */
    val timeRangeString: String get() = "${formatMinuteOfDay(startMinute)} – ${formatMinuteOfDay(endMinute)}"

    companion object {
        fun formatMinuteOfDay(minute: Int): String {
            val clamped = minute.coerceIn(0, 1440)
            val h = clamped / 60
            val m = clamped % 60
            return String.format("%02d:%02d", h, m)
        }
    }
}

/**
 * Entity representing a scheduled or completed study session.
 * Maps to the "sessions" table in SQLite.
 *
 * In Room Schema Version 2, includes exact [startMinute] and [endMinute] for scheduling inside free-time windows.
 */
@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = StudyTask::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("taskId")]
)
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val day: Long,
    val allocatedMinutes: Int,
    val completedMinutes: Int = 0,
    val status: String = PENDING,
    @ColumnInfo(defaultValue = "-1") val startMinute: Int = -1,
    @ColumnInfo(defaultValue = "-1") val endMinute: Int = -1
) {
    val hasExactTimes: Boolean get() = startMinute >= 0 && endMinute > startMinute

    val timeRangeString: String?
        get() = if (hasExactTimes) "${TimeWindow.formatMinuteOfDay(startMinute)} – ${TimeWindow.formatMinuteOfDay(endMinute)}" else null

    companion object {
        const val PENDING = "pending"   // Scheduled session waiting to be studied
        const val CLOSED = "closed"     // Completed study session (completedMinutes > 0)
        const val MISSED = "missed"     // Missed study session (completedMinutes == 0)
    }
}

/**
 * Entity representing local user profile preferences.
 * New in Room Schema Version 2. Stored 100% locally on device.
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Shruthi",
    val studyGoalMinutes: Int = 120,
    val preferredSplit: Boolean = true,
    val avatarIndex: Int = 0
)

/**
 * Single-row table storing a monotonically increasing revision number.
 * Used for optimistic concurrency control: whenever tasks, availability, or progress change,
 * the revision counter is bumped, safely invalidating any outdated recovery preview.
 */
@Entity(tableName = "revision")
data class Revision(@PrimaryKey val id: Int = 1, val value: Long = 0)

/**
 * Data Access Object (DAO) providing asynchronous database operations.
 */
@Dao
interface StudyDao {
    @Query("SELECT * FROM tasks ORDER BY examDay, priority DESC, id")
    suspend fun tasks(): List<StudyTask>

    @Query("SELECT * FROM availability ORDER BY day")
    suspend fun availability(): List<DailyAvailability>

    @Query("SELECT * FROM time_windows ORDER BY day, startMinute, id")
    suspend fun timeWindows(): List<TimeWindow>

    @Query("SELECT * FROM time_windows WHERE day = :day ORDER BY startMinute, id")
    suspend fun timeWindowsForDay(day: Long): List<TimeWindow>

    @Query("SELECT * FROM sessions ORDER BY day, id")
    suspend fun sessions(): List<StudySession>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun task(id: Long): StudyTask?

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun session(id: Long): StudySession?

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun userProfile(): UserProfile?

    @Query("SELECT * FROM course_materials ORDER BY importedAt DESC, id DESC")
    suspend fun courseMaterials(): List<CourseMaterial>

    @Query("SELECT * FROM study_chapters ORDER BY materialId, orderIndex, id")
    suspend fun studyChapters(): List<StudyChapter>

    @Query("SELECT * FROM quiz_questions ORDER BY chapterId, id")
    suspend fun quizQuestions(): List<QuizQuestion>

    @Query("SELECT * FROM quiz_attempts ORDER BY completedAt DESC, id DESC")
    suspend fun quizAttempts(): List<QuizAttempt>

    @Query("SELECT * FROM quiz_attempts WHERE sessionId = :sessionId LIMIT 1")
    suspend fun quizAttemptForSession(sessionId: Long): QuizAttempt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    @Query("SELECT value FROM revision WHERE id = 1")
    fun observeRevision(): Flow<Long?>

    @Query("SELECT value FROM revision WHERE id = 1")
    suspend fun revision(): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initialize(revision: Revision)

    @Query("UPDATE revision SET value = value + 1 WHERE id = 1")
    suspend fun bumpRevision()

    @Insert
    suspend fun insert(task: StudyTask): Long

    @Update
    suspend fun update(task: StudyTask)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAvailability(availability: DailyAvailability)

    @Insert
    suspend fun insertTimeWindows(windows: List<TimeWindow>)

    @Query("DELETE FROM time_windows WHERE day = :day")
    suspend fun clearTimeWindowsForDay(day: Long)

    @Query("DELETE FROM time_windows WHERE id = :id")
    suspend fun deleteTimeWindow(id: Long)

    @Insert
    suspend fun insertSessions(sessions: List<StudySession>)

    @Update
    suspend fun updateSession(session: StudySession)

    @Insert
    suspend fun insertCourseMaterial(material: CourseMaterial): Long

    @Insert
    suspend fun insertStudyChapter(chapter: StudyChapter): Long

    @Insert
    suspend fun insertQuizQuestions(questions: List<QuizQuestion>)

    @Insert
    suspend fun insertQuizAttempt(attempt: QuizAttempt): Long

    @Query("DELETE FROM course_materials WHERE id = :id")
    suspend fun deleteCourseMaterial(id: Long)

    @Query("DELETE FROM tasks WHERE chapterId IN (SELECT id FROM study_chapters WHERE materialId = :materialId)")
    suspend fun deleteTasksForMaterial(materialId: Long)

    @Query("DELETE FROM sessions WHERE status = 'pending' AND day >= :fromDay")
    suspend fun clearPending(fromDay: Long)

    @Query("DELETE FROM sessions WHERE taskId = :taskId AND status = 'pending' AND day >= :fromDay")
    suspend fun clearTaskPending(taskId: Long, fromDay: Long)
}

/**
 * Non-destructive migration from Room schema version 1 to 2.
 * Adds exact time columns to sessions, creates time_windows table, and creates user_profile table.
 * Preserves all existing tasks, availability, and session progress.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions ADD COLUMN startMinute INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE sessions ADD COLUMN endMinute INTEGER NOT NULL DEFAULT -1")
        db.execSQL("CREATE TABLE IF NOT EXISTS `time_windows` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `day` INTEGER NOT NULL, `startMinute` INTEGER NOT NULL, `endMinute` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_time_windows_day` ON `time_windows` (`day`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `studyGoalMinutes` INTEGER NOT NULL, `preferredSplit` INTEGER NOT NULL, `avatarIndex` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("INSERT OR IGNORE INTO `user_profile` (`id`, `name`, `studyGoalMinutes`, `preferredSplit`, `avatarIndex`) VALUES (1, 'Shruthi', 120, 1, 0)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN chapterId INTEGER DEFAULT NULL")
        db.execSQL("CREATE TABLE IF NOT EXISTS `course_materials` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `sourceUri` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `importedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `study_chapters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `materialId` INTEGER NOT NULL, `title` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, FOREIGN KEY(`materialId`) REFERENCES `course_materials`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_chapters_materialId` ON `study_chapters` (`materialId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `quiz_questions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `chapterId` INTEGER NOT NULL, `prompt` TEXT NOT NULL, `options` TEXT NOT NULL, `correctIndex` INTEGER NOT NULL, `explanation` TEXT NOT NULL, `source` TEXT NOT NULL, FOREIGN KEY(`chapterId`) REFERENCES `study_chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_questions_chapterId` ON `quiz_questions` (`chapterId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `quiz_attempts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `chapterId` INTEGER NOT NULL, `sessionId` INTEGER, `score` INTEGER NOT NULL, `total` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, FOREIGN KEY(`chapterId`) REFERENCES `study_chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_attempts_chapterId` ON `quiz_attempts` (`chapterId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_attempts_sessionId` ON `quiz_attempts` (`sessionId`)")
    }
}

/**
 * Main Room Database definition for Ora Rescue.
 * Schema Version 3 adds course materials, chapter-linked topics, quiz questions and attempts.
 */
@Database(
    entities = [
        StudyTask::class,
        DailyAvailability::class,
        StudySession::class,
        Revision::class,
        TimeWindow::class,
        UserProfile::class,
        CourseMaterial::class,
        StudyChapter::class,
        QuizQuestion::class,
        QuizAttempt::class
    ],
    version = 3,
    exportSchema = true
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun dao(): StudyDao

    companion object {
        fun create(context: Context): StudyDatabase = Room.databaseBuilder(
            context.applicationContext,
            StudyDatabase::class.java,
            "ora-rescue.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}
