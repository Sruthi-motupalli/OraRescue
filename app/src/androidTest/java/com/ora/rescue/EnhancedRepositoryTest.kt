package com.ora.rescue

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.util.UUID
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

    @Test fun versionTwoMigrationAddsCoursesWithoutLosingTopics() {
        val name = "ora-v2-course-migration-test"
        migration.createDatabase(name, 2).apply {
            execSQL("INSERT INTO tasks (id, subject, topic, examDay, totalMinutes, priority) VALUES (1, 'OS', 'Scheduling', ${day + 3}, 60, 3)")
            close()
        }
        migration.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3).apply {
            query("SELECT subject, chapterId FROM tasks WHERE id = 1").use {
                assertTrue(it.moveToFirst())
                assertEquals("OS", it.getString(0))
                assertTrue(it.isNull(1))
            }
            query("SELECT COUNT(*) FROM course_materials").use { assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
            close()
        }
    }

    @Test fun savedStudyDataSurvivesClosingAndReopeningRoom() = runBlocking {
        val context = instrumentation.targetContext
        val databaseName = "ora-reopen-${UUID.randomUUID()}.db"
        fun openDatabase() = Room.databaseBuilder(context, StudyDatabase::class.java, databaseName).build()

        try {
            val firstDatabase = openDatabase()
            val saved = try {
                val repository = StudyRepository(firstDatabase)
                repository.saveTask(
                    StudyTask(subject = "OS", topic = "Scheduling", examDay = day + 3, totalMinutes = 90, priority = 3),
                    date
                )
                repository.addTimeWindow(day, 900, 960, date)
                repository.updateProfile("Test student", 90, preferredSplit = false, avatarIndex = 1)
                val plan = Planner.build(repository.snapshot(), date, split = true, currentMinuteOfDay = 900)
                repository.accept(plan, date, currentMinute = 900)
                val session = repository.snapshot().sessions.single()
                repository.recordProgress(session.id, 30, date, currentMinute = 930)
                repository.snapshot()
            } finally {
                firstDatabase.close()
            }
            assertEquals(30, saved.completed(saved.tasks.single().id))

            val reopenedDatabase = openDatabase()
            try {
                assertEquals(saved, StudyRepository(reopenedDatabase).snapshot())
            } finally {
                reopenedDatabase.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
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

    @Test fun courseImportCreatesLinkedTopicsAndPersistentQuizAttempts() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, StudyDatabase::class.java).build()
        try {
            val repo = StudyRepository(db)
            repo.importCourse(
                fileName = "MACHINE-LEARNING.zip",
                sourceUri = "content://test/machine-learning",
                mimeType = "application/zip",
                examDay = day + 30,
                minutesPerChapter = 60,
                today = date
            )
            val imported = repo.snapshot()
            assertEquals(1, imported.courseMaterials.size)
            assertEquals(10, imported.chapters.size)
            assertEquals(10, imported.tasks.size)
            assertTrue(imported.tasks.all { it.chapterId != null })
            val chapter = imported.chapters.first()
            assertEquals(4, imported.questionsForChapter(chapter.id).size)

            repo.saveQuizAttempt(chapter.id, sessionId = null, score = 3, total = 4)
            val attempt = repo.snapshot().bestAttempt(chapter.id)
            assertNotNull(attempt)
            assertEquals(3, attempt?.score)
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
