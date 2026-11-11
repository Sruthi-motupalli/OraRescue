package com.ora.rescue

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnhancedRepositoryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @get:Rule val migration = MigrationTestHelper(instrumentation, StudyDatabase::class.java)
    private val date = LocalDate.of(2026, 11, 9)
    private val day = date.toEpochDay()

    @Test fun versionOneMigrationPreservesLegacyTasksAndProgress() {
        val name = "ora-v1-migration-test"
        migration.createDatabase(name, 1).apply {
            execSQL("INSERT INTO tasks VALUES (1, 'OS', 'Scheduling', ${day + 3}, 60, 3)")
            execSQL("INSERT INTO availability VALUES ($day, 120)")
            execSQL("INSERT INTO sessions VALUES (1, 1, $day, 60, 30, 'closed')")
            close()
        }
        migration.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2).apply {
            query("SELECT completedMinutes, startMinute, endMinute FROM sessions WHERE id = 1").use {
                assertTrue(it.moveToFirst()); assertEquals(30, it.getInt(0)); assertEquals(-1, it.getInt(1)); assertEquals(-1, it.getInt(2))
            }
            query("SELECT minutes FROM availability").use { assertTrue(it.moveToFirst()); assertEquals(120, it.getInt(0)) }
            close()
        }
    }

    @Test fun copyConflictRollsBackAllTargetDatesAndEditingPersists() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, StudyDatabase::class.java).build()
        try {
            val repo = StudyRepository(db)
            repo.addTimeWindow(day, 900, 960, date)
            repo.addTimeWindow(day + 2, 930, 990, date)
            val before = repo.snapshot()
            try { repo.copyWindowsToDates(day, listOf(day + 1, day + 2), date); fail("Expected conflict") } catch (_: IllegalArgumentException) { }
            assertEquals(before, repo.snapshot())
            val id = before.timeWindows.first { it.day == day }.id
            repo.addTimeWindow(day, 960, 1020, date, editId = id)
            assertEquals(960, repo.snapshot().timeWindows.first { it.day == day }.startMinute)
        } finally { db.close() }
    }

    @Test fun passedPreviewStartIsRejectedWithoutChangingSavedData() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, StudyDatabase::class.java).build()
        try {
            val repo = StudyRepository(db)
            repo.saveTask(StudyTask(subject="OS", topic="Scheduling", examDay=day+2, totalMinutes=30, priority=3), date)
            repo.addTimeWindow(day, 900, 960, date)
            val before = repo.snapshot()
            val plan = Planner.build(before, date, true, 900)
            try { repo.accept(plan, date, currentMinute=901); fail("Expected elapsed preview rejection") } catch (_: IllegalArgumentException) { }
            assertEquals(before, repo.snapshot())
            repo.accept(plan, date, currentMinute=900)
            assertEquals(900, repo.snapshot().sessions.single().startMinute)
        } finally { db.close() }
    }
}
