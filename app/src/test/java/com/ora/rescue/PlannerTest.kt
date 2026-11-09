package com.ora.rescue

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests verifying deterministic scheduling behavior in Planner.
 * Tests cover earliest deadline first (EDF), priority ordering, capacity constraints,
 * split vs unsplit heuristics, conservation of work, and expired deadline detection.
 */
class PlannerTest {
    private val today = LocalDate.of(2026, 11, 8)
    private val day = today.toEpochDay()

    private fun task(
        id: Long = 1,
        minutes: Int = 60,
        exam: Long = day + 3,
        priority: Int = 2
    ) = StudyTask(id, "Operating Systems", "Scheduling", exam, minutes, priority)

    @Test
    fun `splits work across days without exceeding capacity`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task()),
            availability = listOf(DailyAvailability(day, 30), DailyAvailability(day + 1, 30))
        )
        val plan = Planner.build(snapshot, today, true)
        assertEquals(listOf(30, 30), plan.sessions.map { it.minutes })
        assertTrue(plan.unplaced.isEmpty())
    }

    @Test
    fun `missing and zero availability produce a visible shortfall`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task()),
            availability = listOf(DailyAvailability(day, 0))
        )
        val plan = Planner.build(snapshot, today, true)
        assertTrue(plan.sessions.isEmpty())
        assertEquals(60, plan.unplaced.single().minutes)
    }

    @Test
    fun `exam day and past capacity are excluded`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task(exam = day + 1)),
            availability = listOf(DailyAvailability(day - 1, 90), DailyAvailability(day + 1, 90))
        )
        assertEquals(60, Planner.build(snapshot, today, true).unplaced.single().minutes)
    }

    @Test
    fun `completed work reduces outstanding work and todays capacity`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task(minutes = 40)),
            availability = listOf(DailyAvailability(day, 30)),
            sessions = listOf(StudySession(1, 1, day, 40, 15, StudySession.CLOSED))
        )
        val plan = Planner.build(snapshot, today, true)
        assertEquals(25, snapshot.remaining(snapshot.tasks.single()))
        assertEquals(15, plan.sessions.single().minutes)
        assertEquals(10, plan.unplaced.single().minutes)
    }

    @Test
    fun `completed tasks do not enter recovery`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task(minutes = 30)),
            availability = listOf(DailyAvailability(day, 90)),
            sessions = listOf(StudySession(1, 1, day - 1, 30, 30, StudySession.CLOSED))
        )
        val plan = Planner.build(snapshot, today, true)
        assertTrue(plan.sessions.isEmpty())
        assertTrue(plan.unplaced.isEmpty())
    }

    @Test
    fun `deadline then priority then id determine a stable order`() {
        val tasks = listOf(
            task(id = 4, minutes = 30, priority = 1),
            task(id = 3, minutes = 30, priority = 3),
            task(id = 2, minutes = 30, priority = 3),
            task(id = 1, minutes = 30, exam = day + 1, priority = 1)
        )
        val snapshot = StudySnapshot(tasks = tasks, availability = listOf(DailyAvailability(day, 120)))
        assertEquals(listOf(1L, 2L, 3L, 4L), Planner.build(snapshot, today, true).sessions.map { it.taskId })
    }

    @Test
    fun `unsplittable work waits for a day where it fits`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task()),
            availability = listOf(DailyAvailability(day, 30), DailyAvailability(day + 1, 60))
        )
        val plan = Planner.build(snapshot, today, false)
        assertEquals(day + 1, plan.sessions.single().day)
        assertEquals(60, plan.sessions.single().minutes)
    }

    @Test
    fun `unsplittable shortfall is not silently split`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task()),
            availability = listOf(DailyAvailability(day, 30), DailyAvailability(day + 1, 30))
        )
        val plan = Planner.build(snapshot, today, false)
        assertTrue(plan.sessions.isEmpty())
        assertEquals(60, plan.unplaced.single().minutes)
    }

    @Test
    fun `old pending allocations are replaced rather than counted twice`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task(minutes = 30)),
            availability = listOf(DailyAvailability(day, 30)),
            sessions = listOf(StudySession(1, 1, day, 30))
        )
        assertEquals(30, Planner.build(snapshot, today, true).sessions.single().minutes)
        assertEquals(1, snapshot.sessions.size)
    }

    @Test
    fun `expired exams remain visible as unallocated work`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task(exam = day)),
            availability = listOf(DailyAvailability(day, 120))
        )
        val plan = Planner.build(snapshot, today, true)
        assertTrue(plan.sessions.isEmpty())
        assertTrue(plan.unplaced.single().reason.contains("today or earlier"))
    }

    @Test
    fun `generated schedules conserve work across varied capacities`() {
        for (minutes in 1..120) {
            val snapshot = StudySnapshot(
                tasks = listOf(task(minutes = minutes)),
                availability = listOf(DailyAvailability(day, 17), DailyAvailability(day + 1, 23))
            )
            val plan = Planner.build(snapshot, today, true)
            assertEquals(minutes, plan.sessions.sumOf { it.minutes } + plan.unplaced.sumOf { it.minutes })
            assertTrue(plan.sessions.filter { it.day == day }.sumOf { it.minutes } <= 17)
            assertTrue(plan.sessions.filter { it.day == day + 1 }.sumOf { it.minutes } <= 23)
        }
    }

    @Test
    fun `multiple tasks competing for scarce slots place higher priority tasks first`() {
        val highPriority = task(id = 10, minutes = 45, exam = day + 2, priority = 3)
        val lowPriority = task(id = 11, minutes = 45, exam = day + 2, priority = 1)
        val snapshot = StudySnapshot(
            tasks = listOf(lowPriority, highPriority),
            availability = listOf(DailyAvailability(day, 45))
        )
        val plan = Planner.build(snapshot, today, true)
        assertEquals(1, plan.sessions.size)
        assertEquals(10L, plan.sessions.single().taskId)
        assertEquals(45, plan.sessions.single().minutes)
        assertEquals(1, plan.unplaced.size)
        assertEquals(11L, plan.unplaced.single().taskId)
        assertTrue(plan.unplaced.single().reason.contains("Not enough available time"))
    }

    @Test
    fun `unsplittable mode provides clear guidance when a topic cannot fit into any single day`() {
        val largeTask = task(id = 20, minutes = 120, exam = day + 3, priority = 2)
        val snapshot = StudySnapshot(
            tasks = listOf(largeTask),
            availability = listOf(DailyAvailability(day, 60), DailyAvailability(day + 1, 60))
        )
        val plan = Planner.build(snapshot, today, false)
        assertTrue(plan.sessions.isEmpty())
        assertEquals(120, plan.unplaced.single().minutes)
        assertTrue(plan.unplaced.single().reason.contains("whole remaining task"))
    }

    @Test
    fun `partial progress on current day correctly accounts for used daily capacity`() {
        val taskA = task(id = 30, minutes = 60, exam = day + 2, priority = 2)
        val taskB = task(id = 31, minutes = 30, exam = day + 2, priority = 2)
        // Day has 60 min capacity, but 40 min already logged as completed on this day
        val snapshot = StudySnapshot(
            tasks = listOf(taskA, taskB),
            availability = listOf(DailyAvailability(day, 60)),
            sessions = listOf(StudySession(id = 1, taskId = 30, day = day, allocatedMinutes = 40, completedMinutes = 40, status = StudySession.CLOSED))
        )
        val plan = Planner.build(snapshot, today, true)
        // 60 - 40 = 20 min available left on `day`
        assertEquals(20, plan.sessions.sumOf { it.minutes })
        // Remaining for taskA is 60 - 40 = 20 min. It gets scheduled for 20 min.
        assertEquals(30, plan.unplaced.single().minutes) // taskB has 30 min unplaced
    }
}
