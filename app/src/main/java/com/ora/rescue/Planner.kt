package com.ora.rescue

import java.time.LocalDate
import java.time.LocalTime

/**
 * Represents a proposed study session placed by the recovery planner on a specific calendar day.
 * Includes exact [startMinute] and [endMinute] (minutes from midnight 0..1440) when scheduled
 * inside a free-time window, or -1 for legacy unscheduled-time sessions.
 */
data class PlannedSession(
    val taskId: Long,
    val day: Long,
    val minutes: Int,
    val startMinute: Int = -1,
    val endMinute: Int = -1
) {
    val hasExactTimes: Boolean get() = startMinute >= 0 && endMinute > startMinute

    val timeRangeString: String?
        get() = if (hasExactTimes) "${TimeWindow.formatMinuteOfDay(startMinute)} – ${TimeWindow.formatMinuteOfDay(endMinute)}" else null
}

/** Represents work that could not be scheduled within available capacity before the exam deadline. */
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
    val unplaced: List<UnplacedWork>,
    val generatedMinute: Int = 0
)

/**
 * An immutable snapshot of all study data at a given point in time.
 * Provides helper functions to calculate completed and remaining study minutes.
 */
data class StudySnapshot(
    val revision: Long = 0,
    val tasks: List<StudyTask> = emptyList(),
    val availability: List<DailyAvailability> = emptyList(),
    val timeWindows: List<TimeWindow> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val courseMaterials: List<CourseMaterial> = emptyList(),
    val chapters: List<StudyChapter> = emptyList(),
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val quizAttempts: List<QuizAttempt> = emptyList()
) {
    /** Sums all completed minutes for a given task across all sessions. */
    fun completed(taskId: Long): Int = sessions.filter { it.taskId == taskId }.sumOf { it.completedMinutes }

    /** Remaining minutes to study = estimated total minutes minus completed minutes. */
    fun remaining(task: StudyTask): Int = (task.totalMinutes - completed(task.id)).coerceAtLeast(0)

    fun chapterForTask(taskId: Long): StudyChapter? = tasks.firstOrNull { it.id == taskId }
        ?.chapterId?.let { chapterId -> chapters.firstOrNull { it.id == chapterId } }

    fun questionsForChapter(chapterId: Long): List<QuizQuestion> = quizQuestions.filter { it.chapterId == chapterId }

    fun bestAttempt(chapterId: Long): QuizAttempt? = quizAttempts.filter { it.chapterId == chapterId }
        .maxWithOrNull(compareBy<QuizAttempt> { it.score.toFloat() / it.total.coerceAtLeast(1) }.thenBy { it.completedAt })

    /**
     * Total configured study availability in minutes for a specific day.
     * Calculated from explicit time windows if configured, falling back to legacy DailyAvailability.
     */
    fun dayCapacity(day: Long): Int {
        val windows = timeWindows.filter { it.day == day }
        return if (windows.isNotEmpty()) {
            windows.sumOf { it.durationMinutes }
        } else {
            availability.find { it.day == day }?.minutes ?: 0
        }
    }
}

/**
 * Deterministic study recovery planner implementing an Earliest-Deadline-First (EDF) heuristic
 * supporting exact free-time windows.
 *
 * Core algorithmic rules:
 * 1. Progress Preservation: Recorded completed study minutes are authoritative and never re-allocated.
 * 2. Exam Deadline Boundary: Study sessions are placed only on days strictly prior to the examDay.
 * 3. Exact Time Windows: When days have configured TimeWindows, sessions are packed into those exact
 *    intervals without overlapping.
 * 4. Elapsed Time Protection: Portions of today's windows prior to [currentMinuteOfDay] are treated as
 *    elapsed and never scheduled into.
 * 5. Splitting Control:
 *    - split = true: Tasks divide across available windows/days until satisfied.
 *    - split = false: An entire task must fit in ONE single continuous free window. Separated windows
 *      are never merged into a fake continuous session.
 * 6. Deterministic Ordering: Unfinished tasks are sorted by:
 *      a) Earliest examDay (urgent deadlines first)
 *      b) Priority descending (Priority 3 High > 2 Medium > 1 Low)
 *      c) Task ID ascending (stable tie-breaker)
 */
object Planner {

    private data class SchedulableSlot(
        val day: Long,
        var currentStart: Int,
        val endMinute: Int,
        var availableMinutes: Int
    ) {
        val isExact: Boolean get() = currentStart >= 0 && endMinute >= 0
    }

    fun build(
        snapshot: StudySnapshot,
        today: LocalDate,
        split: Boolean,
        currentMinuteOfDay: Int = 0
    ): RecoveryPlan {
        require(currentMinuteOfDay in 0..1439) { "Invalid current time." }
        val startDay = today.toEpochDay()

        // Calculate completed minutes per day to prevent double-counting today's already finished work
        val usedMinutesByDay = snapshot.sessions.groupBy { it.day }
            .mapValues { (_, sessions) -> sessions.sumOf { it.completedMinutes } }

        // Build list of schedulable free time intervals across future days
        val candidateDays = (
            snapshot.timeWindows.map { it.day } +
            snapshot.availability.map { it.day }
        ).filter { it >= startDay }.distinct().sorted()

        val slots = mutableListOf<SchedulableSlot>()

        for (day in candidateDays) {
            val dayWindows = snapshot.timeWindows.filter { it.day == day }.sortedBy { it.startMinute }
            var dayCompletedMinutes = usedMinutesByDay[day] ?: 0

            if (dayWindows.isNotEmpty()) {
                // Reserve completed work at its actual position, then clip elapsed time.
                // Otherwise a completed morning session would wrongly consume evening capacity.
                val completed = snapshot.sessions.filter { it.day == day && it.completedMinutes > 0 }
                var unknownUsed = completed.filter { !it.hasExactTimes }.sumOf { it.completedMinutes }
                for (w in dayWindows) {
                    var intervals = listOf(w.startMinute to w.endMinute)
                    for (done in completed.filter { it.hasExactTimes }) {
                        val usedStart = done.startMinute
                        val usedEnd = minOf(done.endMinute, usedStart + done.completedMinutes)
                        intervals = intervals.flatMap { (a, b) ->
                            if (usedStart >= b || usedEnd <= a) listOf(a to b)
                            else listOfNotNull(
                                if (a < usedStart) a to usedStart else null,
                                if (usedEnd < b) usedEnd to b else null
                            )
                        }
                    }
                    for ((a, b) in intervals) {
                        var cursor = if (day == startDay) maxOf(a, currentMinuteOfDay) else a
                        var available = (b - cursor).coerceAtLeast(0)
                        // Legacy records have no clock times; conservatively reserve their minutes.
                        val reserve = minOf(unknownUsed, available)
                        unknownUsed -= reserve
                        cursor += reserve
                        available -= reserve
                        if (available > 0) slots += SchedulableSlot(day, cursor, b, available)
                    }
                }
            } else {
                // Legacy minute-only availability for this day
                val rawCap = snapshot.availability.find { it.day == day }?.minutes ?: 0
                val netCap = (rawCap - dayCompletedMinutes).coerceAtLeast(0)
                if (netCap > 0) {
                    slots += SchedulableSlot(
                        day = day,
                        currentStart = -1,
                        endMinute = -1,
                        availableMinutes = netCap
                    )
                }
            }
        }

        val allocations = mutableListOf<PlannedSession>()
        val unplaced = mutableListOf<UnplacedWork>()

        // Sort tasks: Earliest deadline first, then highest priority, then stable ID
        val orderedTasks = snapshot.tasks.sortedWith(
            compareBy<StudyTask> { it.examDay }.thenByDescending { it.priority }.thenBy { it.id }
        )

        for (task in orderedTasks) {
            var remaining = snapshot.remaining(task)
            if (remaining == 0) continue // Skip completed topics

            if (task.examDay <= startDay) {
                unplaced += UnplacedWork(task.id, remaining, "Exam is today or earlier. Review the exam date.")
                continue
            }

            if (split) {
                // Task may divide across multiple windows and days
                for (slot in slots) {
                    if (slot.day >= task.examDay) continue
                    if (slot.availableMinutes <= 0) continue

                    val allocated = minOf(slot.availableMinutes, remaining)
                    val sStart = if (slot.isExact) slot.currentStart else -1
                    val sEnd = if (slot.isExact) slot.currentStart + allocated else -1

                    allocations += PlannedSession(
                        taskId = task.id,
                        day = slot.day,
                        minutes = allocated,
                        startMinute = sStart,
                        endMinute = sEnd
                    )

                    if (slot.isExact) {
                        slot.currentStart += allocated
                    }
                    slot.availableMinutes -= allocated
                    remaining -= allocated

                    if (remaining == 0) break
                }

                if (remaining > 0) {
                    unplaced += UnplacedWork(
                        taskId = task.id,
                        minutes = remaining,
                        reason = "Not enough available time in free windows before the exam."
                    )
                }
            } else {
                // Unsplittable: The entire remaining task must fit in ONE continuous free window
                val matchingSlot = slots.firstOrNull { slot ->
                    slot.day < task.examDay && slot.availableMinutes >= remaining
                }

                if (matchingSlot != null) {
                    val sStart = if (matchingSlot.isExact) matchingSlot.currentStart else -1
                    val sEnd = if (matchingSlot.isExact) matchingSlot.currentStart + remaining else -1

                    allocations += PlannedSession(
                        taskId = task.id,
                        day = matchingSlot.day,
                        minutes = remaining,
                        startMinute = sStart,
                        endMinute = sEnd
                    )

                    if (matchingSlot.isExact) {
                        matchingSlot.currentStart += remaining
                    }
                    matchingSlot.availableMinutes -= remaining
                    remaining = 0
                } else {
                    unplaced += UnplacedWork(
                        taskId = task.id,
                        minutes = remaining,
                        reason = "No single continuous free window can fit the whole remaining task ($remaining min)."
                    )
                }
            }
        }

        return RecoveryPlan(snapshot.revision, startDay, split, allocations.sortedWith(compareBy<PlannedSession> { it.day }.thenBy { it.startMinute }), unplaced, currentMinuteOfDay)
    }
}
