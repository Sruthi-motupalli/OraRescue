package com.ora.rescue

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ExactWindowTest {
    private val date = LocalDate.of(2026, 11, 9)
    private val day = date.toEpochDay()
    private fun task(id: Long = 1, minutes: Int = 120) = StudyTask(id, "OS", "Scheduling", day + 3, minutes, 2)

    @Test fun completedMorningWorkDoesNotConsumeEveningWindow() {
        val snapshot = StudySnapshot(tasks = listOf(task()),
            timeWindows = listOf(TimeWindow(1, day, 540, 600), TimeWindow(2, day, 1200, 1260)),
            sessions = listOf(StudySession(1, 1, day, 60, 60, StudySession.CLOSED, 540, 600)))
        val plan = Planner.build(snapshot, date, true, 1100)
        assertEquals(1200, plan.sessions.single().startMinute)
        assertEquals(60, plan.sessions.single().minutes)
        assertTrue(plan.unplaced.isEmpty())
    }

    @Test fun exactCompletedIntervalsAreReservedWhereTheyOccurred() {
        val snapshot = StudySnapshot(tasks = listOf(task(minutes = 100)),
            timeWindows = listOf(TimeWindow(1, day, 900, 1020)),
            sessions = listOf(StudySession(1, 1, day, 20, 20, StudySession.CLOSED, 960, 980)))
        val plan = Planner.build(snapshot, date, true, 900)
        assertEquals(listOf(900, 980), plan.sessions.map { it.startMinute })
        assertEquals(listOf(960, 1000), plan.sessions.map { it.endMinute })
        assertEquals(80, plan.sessions.sumOf { it.minutes })
    }

    @Test fun proposalsAreChronologicalEvenWhenLaterTasksFillEarlierGaps() {
        val first = task(1, 60).copy(priority = 3)
        val second = task(2, 30)
        val plan = Planner.build(StudySnapshot(tasks = listOf(first, second), timeWindows = listOf(
            TimeWindow(1, day + 1, 540, 570), TimeWindow(2, day + 1, 900, 960))), date, false, 0)
        assertEquals(listOf(540, 900), plan.sessions.map { it.startMinute })
        assertEquals(listOf(2L, 1L), plan.sessions.map { it.taskId })
    }

    @Test fun previewRetainsClockCutoffForDeterministicAcceptance() {
        val snapshot = StudySnapshot(tasks = listOf(task(minutes = 30)), timeWindows = listOf(TimeWindow(1, day, 900, 960)))
        val plan = Planner.build(snapshot, date, true, 925)
        assertEquals(925, plan.generatedMinute)
        assertEquals(plan, Planner.build(snapshot, date, true, plan.generatedMinute))
        assertEquals(925, plan.sessions.single().startMinute)
    }

    @Test fun editingIgnoresOnlyTheEditedWindow() {
        val windows = listOf(TimeWindow(1, day, 900, 960), TimeWindow(2, day, 1000, 1060))
        assertNull(TimeWindowValidator.validateWindow(day, 910, 970, windows, date, 1))
        assertNotNull(TimeWindowValidator.validateWindow(day, 950, 1030, windows, date, 1))
    }

    @Test fun invalidBoundariesAndDuplicateWindowsAreRejected() {
        assertNotNull(TimeWindowValidator.validateWindow(day, -1, 60, emptyList(), date))
        assertNotNull(TimeWindowValidator.validateWindow(day, 1400, 1450, emptyList(), date))
        assertNotNull(TimeWindowValidator.validateWindow(day, 900, 900, emptyList(), date))
        assertNull(TimeWindowValidator.validateWindow(day, 1380, 1440, emptyList(), date))
        assertNotNull(TimeWindowValidator.validateWindow(day, 900, 960, listOf(TimeWindow(1, day, 900, 960)), date))
    }

    @Test fun competingTopicsNeverOverlapAndConserveAllWork() {
        for (effort in 1..120) {
            val snapshot = StudySnapshot(tasks = listOf(task(1, effort), task(2, 90)), timeWindows = listOf(
                TimeWindow(1, day, 900, 950), TimeWindow(2, day, 1000, 1100)))
            val plan = Planner.build(snapshot, date, true, 920)
            assertEquals(effort + 90, plan.sessions.sumOf { it.minutes } + plan.unplaced.sumOf { it.minutes })
            assertTrue(plan.sessions.all { it.startMinute >= 920 && it.endMinute - it.startMinute == it.minutes })
            assertTrue(plan.sessions.zipWithNext().all { (a, b) -> a.endMinute <= b.startMinute })
        }
    }
}
