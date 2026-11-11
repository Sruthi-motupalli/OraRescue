package com.ora.rescue

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests verifying deterministic scheduling behavior in Planner.
 * Tests cover earliest deadline first (EDF), priority ordering, capacity constraints,
 * split vs unsplit heuristics, conservation of work, exact free-time windows,
 * and elapsed real-time handling.
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
        val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 0)
        assertEquals(listOf(30, 30), plan.sessions.map { it.minutes })
        assertTrue(plan.unplaced.isEmpty())
    }

    @Test
    fun `missing and zero availability produce a visible shortfall`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task()),
            availability = listOf(DailyAvailability(day, 0))
        )
        val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 0)
        assertTrue(plan.sessions.isEmpty())
        assertEquals(60, plan.unplaced.single().minutes)
    }

    @Test
    fun `exam day and past capacity are excluded`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task(exam = day + 1)),
            availability = listOf(DailyAvailability(day - 1, 90), DailyAvailability(day + 1, 90))
        )
        assertEquals(60, Planner.build(snapshot, today, true, currentMinuteOfDay = 0).unplaced.single().minutes)
    }

    @Test
    fun `completed work reduces outstanding work and todays capacity`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task(minutes = 40)),
            availability = listOf(DailyAvailability(day, 30)),
            sessions = listOf(StudySession(1, 1, day, 40, 15, StudySession.CLOSED))
        )
        val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 0)
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
        val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 0)
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
        assertEquals(listOf(1L, 2L, 3L, 4L), Planner.build(snapshot, today, true, currentMinuteOfDay = 0).sessions.map { it.taskId })
    }

    @Test
    fun `unsplittable work waits for a day where it fits`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task()),
            availability = listOf(DailyAvailability(day, 30), DailyAvailability(day + 1, 60))
        )
        val plan = Planner.build(snapshot, today, false, currentMinuteOfDay = 0)
        assertEquals(day + 1, plan.sessions.single().day)
        assertEquals(60, plan.sessions.single().minutes)
    }

    @Test
    fun `unsplittable shortfall is not silently split`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task()),
            availability = listOf(DailyAvailability(day, 30), DailyAvailability(day + 1, 30))
        )
        val plan = Planner.build(snapshot, today, false, currentMinuteOfDay = 0)
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
        assertEquals(30, Planner.build(snapshot, today, true, currentMinuteOfDay = 0).sessions.single().minutes)
        assertEquals(1, snapshot.sessions.size)
    }

    @Test
    fun `expired exams remain visible as unallocated work`() {
        val snapshot = StudySnapshot(
            tasks = listOf(task(exam = day)),
            availability = listOf(DailyAvailability(day, 120))
        )
        val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 0)
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
            val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 0)
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
        val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 0)
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
        val plan = Planner.build(snapshot, today, false, currentMinuteOfDay = 0)
        assertTrue(plan.sessions.isEmpty())
        assertEquals(120, plan.unplaced.single().minutes)
        assertTrue(plan.unplaced.single().reason.contains("whole remaining task"))
    }

    @Test
    fun `partial progress on current day correctly accounts for used daily capacity`() {
        val taskA = task(id = 30, minutes = 60, exam = day + 2, priority = 2)
        val taskB = task(id = 31, minutes = 30, exam = day + 2, priority = 2)
        val snapshot = StudySnapshot(
            tasks = listOf(taskA, taskB),
            availability = listOf(DailyAvailability(day, 60)),
            sessions = listOf(StudySession(id = 1, taskId = 30, day = day, allocatedMinutes = 40, completedMinutes = 40, status = StudySession.CLOSED))
        )
        val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 0)
        assertEquals(20, plan.sessions.sumOf { it.minutes })
        assertEquals(30, plan.unplaced.single().minutes)
    }

    // --- NEW FOCUSED TESTS FOR MULTIPLE WINDOWS, EXACT TIMES & ELAPSED TIME ---

    @Test
    fun `schedules tasks into multiple exact free-time windows with exact start and end times`() {
        val t1 = task(id = 101, minutes = 60, exam = day + 2, priority = 3)
        val t2 = task(id = 102, minutes = 90, exam = day + 2, priority = 2)
        // Two windows on day + 1: 16:00-17:00 (960-1020, 60m) and 20:00-21:30 (1200-1290, 90m)
        val windows = listOf(
            TimeWindow(id = 1, day = day + 1, startMinute = 960, endMinute = 1020),
            TimeWindow(id = 2, day = day + 1, startMinute = 1200, endMinute = 1290)
        )
        val snapshot = StudySnapshot(tasks = listOf(t1, t2), timeWindows = windows)
        val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 0)

        assertEquals(2, plan.sessions.size)
        // t1 (higher priority) gets the earlier window: 960 to 1020 (60 min)
        val s1 = plan.sessions[0]
        assertEquals(101L, s1.taskId)
        assertEquals(960, s1.startMinute)
        assertEquals(1020, s1.endMinute)
        assertEquals(60, s1.minutes)

        // t2 gets the second window: 1200 to 1290 (90 min)
        val s2 = plan.sessions[1]
        assertEquals(102L, s2.taskId)
        assertEquals(1200, s2.startMinute)
        assertEquals(1290, s2.endMinute)
        assertEquals(90, s2.minutes)

        assertTrue(plan.unplaced.isEmpty())
    }

    @Test
    fun `unsplittable mode requires one single continuous window and rejects spanning across separated windows`() {
        val largeTask = task(id = 103, minutes = 90, exam = day + 2, priority = 2)
        // Two separated 60-minute windows: 16:00-17:00 (60m) and 20:00-21:00 (60m). Total is 120m, but neither is >= 90m.
        val windows = listOf(
            TimeWindow(id = 1, day = day + 1, startMinute = 960, endMinute = 1020),
            TimeWindow(id = 2, day = day + 1, startMinute = 1200, endMinute = 1260)
        )
        val snapshot = StudySnapshot(tasks = listOf(largeTask), timeWindows = windows)
        val plan = Planner.build(snapshot, today, false, currentMinuteOfDay = 0)

        // Must NOT combine separated windows into a fake continuous session!
        assertTrue(plan.sessions.isEmpty())
        assertEquals(90, plan.unplaced.single().minutes)
        assertTrue(plan.unplaced.single().reason.contains("No single continuous free window"))
    }

    @Test
    fun `split mode allows task to span across multiple separate windows with exact times`() {
        val taskToSplit = task(id = 104, minutes = 90, exam = day + 2, priority = 2)
        // Two separated 60-minute windows: 16:00-17:00 (60m) and 20:00-21:00 (60m).
        val windows = listOf(
            TimeWindow(id = 1, day = day + 1, startMinute = 960, endMinute = 1020),
            TimeWindow(id = 2, day = day + 1, startMinute = 1200, endMinute = 1260)
        )
        val snapshot = StudySnapshot(tasks = listOf(taskToSplit), timeWindows = windows)
        val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 0)

        assertEquals(2, plan.sessions.size)
        // Part 1: fills first window 960..1020 (60m)
        assertEquals(960, plan.sessions[0].startMinute)
        assertEquals(1020, plan.sessions[0].endMinute)
        assertEquals(60, plan.sessions[0].minutes)

        // Part 2: takes 30m of second window: 1200..1230 (30m)
        assertEquals(1200, plan.sessions[1].startMinute)
        assertEquals(1230, plan.sessions[1].endMinute)
        assertEquals(30, plan.sessions[1].minutes)

        assertTrue(plan.unplaced.isEmpty())
    }

    @Test
    fun `elapsed windows earlier today are excluded from recovery`() {
        val t = task(id = 105, minutes = 60, exam = day + 2, priority = 2)
        // Two windows today: 09:00-10:00 (540-600) and 16:00-17:00 (960-1020).
        // Current time is 14:00 (840). Window 1 has completely elapsed!
        val windows = listOf(
            TimeWindow(id = 1, day = day, startMinute = 540, endMinute = 600),
            TimeWindow(id = 2, day = day, startMinute = 960, endMinute = 1020)
        )
        val snapshot = StudySnapshot(tasks = listOf(t), timeWindows = windows)
        val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 840) // 14:00

        assertEquals(1, plan.sessions.size)
        val session = plan.sessions.single()
        assertEquals(960, session.startMinute)
        assertEquals(1020, session.endMinute)
        assertEquals(60, session.minutes)
    }

    @Test
    fun `partially elapsed window earlier today has only remaining portion scheduled`() {
        val t = task(id = 106, minutes = 60, exam = day + 2, priority = 2)
        // Window today: 16:00-18:00 (960-1080, 120m).
        // Current time is 16:30 (990). Only 990-1080 (90m) is available!
        val windows = listOf(
            TimeWindow(id = 1, day = day, startMinute = 960, endMinute = 1080)
        )
        val snapshot = StudySnapshot(tasks = listOf(t), timeWindows = windows)
        val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 990) // 16:30

        val session = plan.sessions.single()
        assertEquals(990, session.startMinute)
        assertEquals(1050, session.endMinute)
        assertEquals(60, session.minutes)
    }

    @Test
    fun `adjacent windows on same day are scheduled without gap or overlap`() {
        val t = task(id = 107, minutes = 120, exam = day + 2, priority = 2)
        // Adjacent windows: 16:00-17:00 (960-1020) and 17:00-18:00 (1020-1080).
        val windows = listOf(
            TimeWindow(id = 1, day = day + 1, startMinute = 960, endMinute = 1020),
            TimeWindow(id = 2, day = day + 1, startMinute = 1020, endMinute = 1080)
        )
        val snapshot = StudySnapshot(tasks = listOf(t), timeWindows = windows)
        val plan = Planner.build(snapshot, today, true, currentMinuteOfDay = 0)

        assertEquals(2, plan.sessions.size)
        assertEquals(960, plan.sessions[0].startMinute)
        assertEquals(1020, plan.sessions[0].endMinute)
        assertEquals(1020, plan.sessions[1].startMinute)
        assertEquals(1080, plan.sessions[1].endMinute)
        assertEquals(120, plan.sessions.sumOf { it.minutes })
    }

    @Test
    fun `TimeWindowValidator rejects overlapping windows and backwards times but accepts adjacent windows`() {
        val existing = listOf(
            TimeWindow(id = 1, day = day + 1, startMinute = 960, endMinute = 1020) // 16:00-17:00
        )

        // Adjacent before: 15:00-16:00 (900-960) -> Valid!
        assertNull(TimeWindowValidator.validateWindow(day + 1, 900, 960, existing, today))

        // Adjacent after: 17:00-18:00 (1020-1080) -> Valid!
        assertNull(TimeWindowValidator.validateWindow(day + 1, 1020, 1080, existing, today))

        // Overlapping interior: 16:30-17:30 (990-1050) -> Rejected!
        val overlapErr = TimeWindowValidator.validateWindow(day + 1, 990, 1050, existing, today)
        assertNotNull(overlapErr)
        assertTrue(overlapErr!!.contains("overlaps"))

        // Duplicate: 16:00-17:00 (960-1020) -> Rejected!
        val dupErr = TimeWindowValidator.validateWindow(day + 1, 960, 1020, existing, today)
        assertNotNull(dupErr)

        // Backwards time: 18:00-16:00 (1080-960) -> Rejected!
        val backErr = TimeWindowValidator.validateWindow(day + 1, 1080, 960, existing, today)
        assertNotNull(backErr)
        assertTrue(backErr!!.contains("earlier than end time"))

        // Past date -> Rejected!
        val pastErr = TimeWindowValidator.validateWindow(day - 1, 900, 960, existing, today)
        assertNotNull(pastErr)
        assertTrue(pastErr!!.contains("past dates"))
    }
}
