package com.ora.rescue

import java.time.LocalDate

/** Represents a proposed study session placed by the recovery planner on a specific calendar day. */
data class PlannedSession(val taskId: Long, val day: Long, val minutes: Int)

/** Represents work that could not be scheduled within available daily capacity before the exam deadline. */
data class UnplacedWork(val taskId: Long, val minutes: Int, val reason: String)

/**
 * An immutable recovery plan generated for preview.
 * Includes the database revision and startDay at generation time to detect stale data.
 */
data class RecoveryPlan(
    val revision: Long,
    val startDay: Long,
    val splitTasks: Boolean,
    val sessions: List<PlannedSession>,
    val unplaced: List<UnplacedWork>
)

/**
 * An immutable snapshot of all study data at a given point in time.
 * Provides helper functions to calculate completed and remaining study minutes.
 */
data class StudySnapshot(
    val revision: Long = 0,
    val tasks: List<StudyTask> = emptyList(),
    val availability: List<DailyAvailability> = emptyList(),
    val sessions: List<StudySession> = emptyList()
) {
    /** Sums all completed minutes for a given task across all sessions. */
    fun completed(taskId: Long): Int = sessions.filter { it.taskId == taskId }.sumOf { it.completedMinutes }

    /** Remaining minutes to study = estimated total minutes minus completed minutes. */
    fun remaining(task: StudyTask): Int = (task.totalMinutes - completed(task.id)).coerceAtLeast(0)
}

/**
 * Deterministic study recovery planner implementing an Earliest-Deadline-First (EDF) heuristic.
 *
 * Key algorithmic principles for student explanation:
 * 1. Progress Preservation: Recorded completed study minutes are authoritative and never re-allocated.
 * 2. Exam Deadline Boundary: Study sessions are only placed on days strictly prior to the examDay.
 * 3. Daily Capacity Limits: Uses explicitly configured DailyAvailability minus any sessions already
 *    completed on that day. Days without configured availability have 0 capacity.
 * 4. Deterministic Ordering: Unfinished tasks are sorted by:
 *      a) Earliest examDay (urgent deadlines first)
 *      b) Priority descending (Priority 3 High > Priority 2 Medium > Priority 1 Low)
 *      c) Task ID ascending (tie-breaker ensuring deterministic results)
 * 5. Splitting Strategy:
 *      - split = true: Allocates whatever minutes fit into each eligible day until the topic is satisfied.
 *      - split = false: Greedily seeks the earliest eligible day with enough remaining capacity for the full task.
 */
object Planner {
    fun build(snapshot: StudySnapshot, today: LocalDate, split: Boolean): RecoveryPlan {
        val start = today.toEpochDay()

        // Sum minutes already completed on each day so today's logged work isn't double-booked
        val used = snapshot.sessions.groupBy { it.day }
            .mapValues { (_, sessions) -> sessions.sumOf { it.completedMinutes } }

        // Filter configured availability to today and future days, subtracting already completed minutes
        val capacity = snapshot.availability.filter { it.day >= start }.associate {
            it.day to (it.minutes - (used[it.day] ?: 0)).coerceAtLeast(0)
        }.toSortedMap()

        val allocations = mutableListOf<PlannedSession>()
        val unplaced = mutableListOf<UnplacedWork>()

        // Sort tasks: Earliest deadline first, then highest priority, then stable ID
        val orderedTasks = snapshot.tasks.sortedWith(
            compareBy<StudyTask> { it.examDay }.thenByDescending { it.priority }.thenBy { it.id }
        )

        for (task in orderedTasks) {
            var remaining = snapshot.remaining(task)
            if (remaining == 0) continue // Skip completed topics

            if (task.examDay <= start) {
                unplaced += UnplacedWork(task.id, remaining, "Exam is today or earlier. Review the exam date.")
                continue
            }

            // Iterate through days strictly before the exam deadline
            for (day in capacity.keys.filter { it < task.examDay }) {
                val available = capacity.getValue(day)
                if (available == 0 || (!split && available < remaining)) continue

                val allocated = minOf(available, remaining)
                allocations += PlannedSession(task.id, day, allocated)
                capacity[day] = available - allocated
                remaining -= allocated
                if (remaining == 0) break
            }

            // If any work remains unplaced, record informative reason for user feedback
            if (remaining > 0) {
                val reason = if (split) {
                    "Not enough available time before the exam."
                } else {
                    "No eligible day can fit the whole remaining task."
                }
                unplaced += UnplacedWork(task.id, remaining, reason)
            }
        }

        return RecoveryPlan(snapshot.revision, start, split, allocations, unplaced)
    }
}
