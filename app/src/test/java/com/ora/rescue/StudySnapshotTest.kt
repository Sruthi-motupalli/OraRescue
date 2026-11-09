package com.ora.rescue

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests verifying business calculations and snapshot integrity.
 */
class StudySnapshotTest {
    private val today = LocalDate.of(2026, 11, 8)
    private val day = today.toEpochDay()

    @Test
    fun `snapshot correctly calculates completed minutes across sessions`() {
        val task = StudyTask(id = 1, subject = "Math", topic = "Linear Algebra", examDay = day + 5, totalMinutes = 120, priority = 2)
        val sessions = listOf(
            StudySession(id = 10, taskId = 1, day = day - 2, allocatedMinutes = 30, completedMinutes = 30, status = StudySession.CLOSED),
            StudySession(id = 11, taskId = 1, day = day - 1, allocatedMinutes = 45, completedMinutes = 20, status = StudySession.CLOSED),
            StudySession(id = 12, taskId = 1, day = day, allocatedMinutes = 30, completedMinutes = 0, status = StudySession.PENDING)
        )
        val snapshot = StudySnapshot(tasks = listOf(task), sessions = sessions)
        assertEquals(50, snapshot.completed(1))
        assertEquals(70, snapshot.remaining(task))
    }

    @Test
    fun `snapshot remaining minutes is never negative even if extra completed`() {
        val task = StudyTask(id = 2, subject = "CS", topic = "Algorithms", examDay = day + 3, totalMinutes = 60, priority = 3)
        val sessions = listOf(
            StudySession(id = 20, taskId = 2, day = day, allocatedMinutes = 60, completedMinutes = 70, status = StudySession.CLOSED)
        )
        val snapshot = StudySnapshot(tasks = listOf(task), sessions = sessions)
        assertEquals(70, snapshot.completed(2))
        assertEquals(0, snapshot.remaining(task))
    }

    @Test
    fun `recovery plan equality checks reject plans with different revisions or days`() {
        val planA = RecoveryPlan(revision = 1L, startDay = day, splitTasks = true, sessions = emptyList(), unplaced = emptyList())
        val planB = RecoveryPlan(revision = 2L, startDay = day, splitTasks = true, sessions = emptyList(), unplaced = emptyList())
        val planC = RecoveryPlan(revision = 1L, startDay = day + 1, splitTasks = true, sessions = emptyList(), unplaced = emptyList())
        val planD = RecoveryPlan(revision = 1L, startDay = day, splitTasks = false, sessions = emptyList(), unplaced = emptyList())
        val planCopy = RecoveryPlan(revision = 1L, startDay = day, splitTasks = true, sessions = emptyList(), unplaced = emptyList())

        assertEquals(planA, planCopy)
        assertNotEquals(planA, planB)
        assertNotEquals(planA, planC)
        assertNotEquals(planA, planD)
    }

    @Test
    fun `priority labels and helper functions return readable values`() {
        assertEquals("High", priorityLabel(3))
        assertEquals("Medium", priorityLabel(2))
        assertEquals("Low", priorityLabel(1))
        assertEquals("Low", priorityLabel(0))
        assertEquals("Exam tomorrow", daysRemainingLabel(day + 1, day))
        assertEquals("Exam is today", daysRemainingLabel(day, day))
        assertEquals("Exam in 5 days", daysRemainingLabel(day + 5, day))
        assertEquals("Exam date passed", daysRemainingLabel(day - 1, day))
    }
}

