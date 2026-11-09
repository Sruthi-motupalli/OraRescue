package com.ora.rescue

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Run on an Android emulator or device from Android Studio. */
@RunWith(AndroidJUnit4::class)
class StudyRepositoryTest {
    private lateinit var database: StudyDatabase
    private lateinit var repository: StudyRepository
    private val today = LocalDate.of(2026, 11, 8)
    private val day = today.toEpochDay()
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(context, StudyDatabase::class.java).build()
        repository = StudyRepository(database)
    }

    @After fun close() { database.close() }

    private suspend fun preparedPlan(): RecoveryPlan {
        repository.saveTask(StudyTask(subject = "OS", topic = "Scheduling", examDay = day + 2, totalMinutes = 60, priority = 3), today)
        repository.availability(day, 30, today)
        repository.availability(day + 1, 30, today)
        return Planner.build(repository.snapshot(), today, true)
    }

    @Test fun editAndDeleteKeepRecordsConsistent() = runBlocking {
        val plan = preparedPlan()
        repository.accept(plan, today)
        val task = repository.snapshot().tasks.single()
        repository.saveTask(task.copy(totalMinutes = 40), today)
        assertTrue(repository.snapshot().sessions.isEmpty())
        assertEquals(40, repository.snapshot().remaining(repository.snapshot().tasks.single()))
        repository.deleteTask(task.id, today)
        assertTrue(repository.snapshot().tasks.isEmpty())
        assertTrue(repository.snapshot().sessions.isEmpty())
    }

    @Test fun changedInputsAndRepeatedAcceptanceAreRejected() = runBlocking {
        val plan = preparedPlan()
        repository.availability(day, 20, today)
        assertThrowsSuspend { repository.accept(plan, today) }
        val fresh = Planner.build(repository.snapshot(), today, true)
        repository.accept(fresh, today)
        assertThrowsSuspend { repository.accept(fresh, today) }
        assertEquals(fresh.sessions.size, repository.snapshot().sessions.size)
    }

    @Test fun progressIsRecordedOnceAndFutureTaskAllocationsAreReconciled() = runBlocking {
        val plan = preparedPlan()
        repository.accept(plan, today)
        val session = repository.snapshot().sessions.first { it.day == day }
        repository.recordProgress(session.id, 15, today)
        assertThrowsSuspend { repository.recordProgress(session.id, 15, today) }
        val snapshot = repository.snapshot()
        assertEquals(45, snapshot.remaining(snapshot.tasks.single()))
        assertEquals(15, snapshot.sessions.sumOf { it.completedMinutes })
        assertTrue(snapshot.sessions.none { it.status == StudySession.PENDING })
        val recovered = Planner.build(snapshot, today, true)
        assertEquals(15, recovered.sessions.filter { it.day == day }.sumOf { it.minutes })
    }

    @Test fun validationDoesNotPartiallySaveChanges() = runBlocking {
        preparedPlan()
        val before = repository.snapshot()
        assertThrowsSuspend { repository.availability(day, -1, today) }
        assertThrowsSuspend { repository.saveTask(before.tasks.single().copy(totalMinutes = 0), today) }
        assertEquals(before, repository.snapshot())
    }

    @Test fun taskAvailabilityAndProgressSurviveDatabaseReopen() = runBlocking {
        database.close()
        val name = "ora-repository-verification.db"
        context.deleteDatabase(name)
        database = Room.databaseBuilder(context, StudyDatabase::class.java, name).build()
        repository = StudyRepository(database)
        try {
            val plan = preparedPlan()
            repository.accept(plan, today)
            repository.recordProgress(repository.snapshot().sessions.first().id, 10, today)
            val before = repository.snapshot()
            database.close()
            database = Room.databaseBuilder(context, StudyDatabase::class.java, name).build()
            repository = StudyRepository(database)
            assertEquals(before, repository.snapshot())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private suspend fun assertThrowsSuspend(action: suspend () -> Unit) {
        try {
            action()
            fail("Expected invalid operation to be rejected")
        } catch (_: IllegalArgumentException) { }
    }
}
