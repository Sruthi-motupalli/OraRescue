package com.ora.rescue

import android.content.Context
import androidx.room.*
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
    val priority: Int
)

/**
 * Entity representing user-configured available study minutes for a specific calendar day.
 * Maps to the "availability" table in SQLite.
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
 * Entity representing a scheduled or completed study session.
 * Maps to the "sessions" table in SQLite.
 *
 * Has a Foreign Key relationship with StudyTask:
 * - If a task is deleted, its sessions are automatically removed (CASCADE).
 * - Indexed on taskId to ensure efficient join and lookup queries.
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
    val status: String = PENDING
) {
    companion object {
        const val PENDING = "pending"   // Scheduled session waiting to be studied
        const val CLOSED = "closed"     // Completed study session (completedMinutes > 0)
        const val MISSED = "missed"     // Missed study session (completedMinutes == 0)
    }
}

/**
 * Single-row table storing a monotonically increasing revision number.
 * Used for optimistic concurrency control: whenever tasks, availability, or progress change,
 * the revision counter is bumped, safely invalidating any outdated recovery preview.
 */
@Entity(tableName = "revision")
data class Revision(@PrimaryKey val id: Int = 1, val value: Long = 0)

/**
 * Data Access Object (DAO) providing asynchronous database operations.
 * Methods are marked with suspend for execution inside Coroutines or return Flow for live observation.
 */
@Dao
interface StudyDao {
    @Query("SELECT * FROM tasks ORDER BY examDay, priority DESC, id")
    suspend fun tasks(): List<StudyTask>

    @Query("SELECT * FROM availability ORDER BY day")
    suspend fun availability(): List<DailyAvailability>

    @Query("SELECT * FROM sessions ORDER BY day, id")
    suspend fun sessions(): List<StudySession>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun task(id: Long): StudyTask?

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun session(id: Long): StudySession?

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
    suspend fun insertSessions(sessions: List<StudySession>)

    @Update
    suspend fun updateSession(session: StudySession)

    @Query("DELETE FROM sessions WHERE status = 'pending' AND day >= :fromDay")
    suspend fun clearPending(fromDay: Long)

    @Query("DELETE FROM sessions WHERE taskId = :taskId AND status = 'pending' AND day >= :fromDay")
    suspend fun clearTaskPending(taskId: Long, fromDay: Long)
}

/**
 * Main Room Database definition for Ora Rescue.
 * Declares entities, schema version, and exposes the DAO.
 */
@Database(
    entities = [StudyTask::class, DailyAvailability::class, StudySession::class, Revision::class],
    version = 1,
    exportSchema = true
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun dao(): StudyDao

    companion object {
        fun create(context: Context): StudyDatabase = Room.databaseBuilder(
            context.applicationContext,
            StudyDatabase::class.java,
            "ora-rescue.db"
        ).build()
    }
}
