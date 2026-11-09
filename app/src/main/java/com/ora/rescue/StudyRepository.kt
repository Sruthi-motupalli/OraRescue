package com.ora.rescue

import androidx.room.withTransaction
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository providing a clean data access layer for Ora Rescue.
 * Encapsulates Room database transactions, business validation, and atomic revision tracking.
 *
 * All mutating operations are wrapped in Room transactions (`database.withTransaction`) to guarantee:
 * - Atomicity: Partial changes are never saved if validation or an error occurs.
 * - Consistency: Future pending sessions are purged when inputs change, preventing stale schedules.
 * - Concurrency Safety: Revision counter increments notify observers of state changes.
 */
class StudyRepository(private val database: StudyDatabase) {
    private val dao = database.dao()

    /** Live flow emitting a fresh StudySnapshot whenever the revision counter updates. */
    val snapshots: Flow<StudySnapshot> = dao.observeRevision().map { snapshot() }

    /** Reads a complete, consistent snapshot of all tasks, availability, and sessions. */
    suspend fun snapshot(): StudySnapshot = database.withTransaction { readSnapshot() }

    private suspend fun readSnapshot() = StudySnapshot(
        revision = dao.revision() ?: 0,
        tasks = dao.tasks(),
        availability = dao.availability(),
        sessions = dao.sessions()
    )

    /**
     * Creates or updates a study topic with strict business rule validation.
     * On save, clears any future pending sessions from today onward to invalidate outdated plans.
     */
    suspend fun saveTask(task: StudyTask, today: LocalDate = LocalDate.now()) {
        require(task.subject.isNotBlank() && task.topic.isNotBlank()) { "Enter a subject and study topic." }
        require(task.totalMinutes in 1..100000) { "Enter between 1 and 100,000 minutes." }
        require(task.priority in 1..3) { "Choose a priority." }
        require(task.examDay > today.toEpochDay()) { "Choose an exam date after today." }

        database.withTransaction {
            dao.initialize(Revision())
            val snapshot = readSnapshot()
            require(task.id == 0L || snapshot.tasks.any { it.id == task.id }) { "This task was deleted. Return home." }
            require(task.totalMinutes >= snapshot.completed(task.id)) { "Total time cannot be below work already completed." }

            if (task.id == 0L) dao.insert(task) else dao.update(task)

            // Discard obsolete future allocations from today onward. Historical completed progress is preserved.
            dao.clearPending(today.toEpochDay())
            dao.bumpRevision()
        }
    }

    /**
     * Deletes a study topic by ID. Cascades to delete all its associated sessions via SQLite foreign keys.
     */
    suspend fun deleteTask(id: Long, today: LocalDate = LocalDate.now()) {
        database.withTransaction {
            dao.initialize(Revision())
            dao.deleteTask(id)
            dao.clearPending(today.toEpochDay())
            dao.bumpRevision()
        }
    }

    /**
     * Configures available study minutes for a given calendar day (0 to 1,440 minutes / 24 hours).
     */
    suspend fun availability(day: Long, minutes: Int, today: LocalDate = LocalDate.now()) {
        val todayDay = today.toEpochDay()
        require(day >= todayDay) { "Availability can only be configured for today or a future day." }
        require(minutes in 0..1440) { "Enter between 0 and 1,440 minutes." }

        database.withTransaction {
            dao.initialize(Revision())
            val used = dao.sessions().filter { it.day == day }.sumOf { it.completedMinutes }
            require(minutes >= used) { "Availability cannot be below the $used minutes already completed on this day." }

            dao.saveAvailability(DailyAvailability(day, minutes))
            dao.clearPending(todayDay)
            dao.bumpRevision()
        }
    }

    /**
     * Records progress for a scheduled study session.
     * Can only be recorded on or after the scheduled session day.
     * A partially or fully completed session closes permanently; future recovery schedules remaining work.
     */
    suspend fun recordProgress(sessionId: Long, minutes: Int, today: LocalDate = LocalDate.now()) {
        val todayDay = today.toEpochDay()
        database.withTransaction {
            dao.initialize(Revision())
            val session = requireNotNull(dao.session(sessionId)) { "This session changed. Refresh the plan." }
            require(session.status == StudySession.PENDING) { "Progress for this session is already recorded." }
            require(session.day <= todayDay) { "Record progress on the study day or afterwards." }

            val task = requireNotNull(dao.task(session.taskId)) { "Task no longer exists." }
            val remaining = readSnapshot().remaining(task)
            require(minutes in 0..minOf(session.allocatedMinutes, remaining)) { "Enter valid completed minutes for this session." }

            dao.updateSession(
                session.copy(
                    completedMinutes = minutes,
                    status = if (minutes == 0) StudySession.MISSED else StudySession.CLOSED
                )
            )

            // Clear subsequent pending allocations for this task so recovery can re-plan remaining work
            dao.clearTaskPending(task.id, todayDay)
            dao.bumpRevision()
        }
    }

    /**
     * Atomically commits an accepted recovery plan to the database.
     * Strictly verifies that the database has not changed since the preview was generated.
     */
    suspend fun accept(plan: RecoveryPlan, today: LocalDate = LocalDate.now()) {
        val todayDay = today.toEpochDay()
        database.withTransaction {
            dao.initialize(Revision())
            require(plan.startDay == todayDay) { "The date changed. Generate a fresh preview." }
            require((dao.revision() ?: 0) == plan.revision) { "Your tasks or availability changed. Generate a new preview." }

            val expected = Planner.build(readSnapshot(), today, plan.splitTasks)
            require(plan == expected) { "This preview is out of date. Generate it again." }

            dao.clearPending(todayDay)
            dao.insertSessions(
                plan.sessions.map {
                    StudySession(
                        taskId = it.taskId,
                        day = it.day,
                        allocatedMinutes = it.minutes
                    )
                }
            )
            dao.bumpRevision()
        }
    }
}
