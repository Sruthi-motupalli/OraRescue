package com.ora.rescue

import androidx.room.withTransaction
import java.time.LocalDate
import java.time.LocalTime
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

    /** Reads a complete, consistent snapshot of all tasks, availability, time windows, and sessions. */
    suspend fun snapshot(): StudySnapshot = database.withTransaction { readSnapshot() }

    private suspend fun readSnapshot(): StudySnapshot {
        val rev = dao.revision() ?: 0
        val tasks = dao.tasks()
        val avail = dao.availability()
        val windows = dao.timeWindows()
        val sessions = dao.sessions()
        val profile = dao.userProfile() ?: UserProfile()
        val courseMaterials = dao.courseMaterials()
        val chapters = dao.studyChapters()
        val quizQuestions = dao.quizQuestions()
        val quizAttempts = dao.quizAttempts()
        return StudySnapshot(
            revision = rev,
            tasks = tasks,
            availability = avail,
            timeWindows = windows,
            sessions = sessions,
            profile = profile,
            courseMaterials = courseMaterials,
            chapters = chapters,
            quizQuestions = quizQuestions,
            quizAttempts = quizAttempts
        )
    }

    data class ImportedCourse(val materialId: Long, val firstTaskId: Long, val firstChapterId: Long)

    suspend fun importCourse(
        fileName: String,
        sourceUri: String,
        mimeType: String,
        examDay: Long,
        minutesPerChapter: Int,
        today: LocalDate = LocalDate.now()
    ): ImportedCourse {
        require(fileName.isNotBlank()) { "Choose a course material file." }
        require(examDay > today.toEpochDay()) { "Choose an exam date after today." }
        require(minutesPerChapter in 15..1440) { "Choose 15 to 1,440 minutes per chapter." }
        val chapterTitles = CourseTemplates.chaptersFor(fileName)
        require(chapterTitles.isNotEmpty()) { "No chapters were found." }

        return database.withTransaction {
            dao.initialize(Revision())
            val materialId = dao.insertCourseMaterial(
                CourseMaterial(
                    name = fileName.substringBeforeLast('.').ifBlank { fileName },
                    sourceUri = sourceUri,
                    mimeType = mimeType,
                    importedAt = System.currentTimeMillis()
                )
            )
            var firstTaskId = 0L
            var firstChapterId = 0L
            chapterTitles.forEachIndexed { index, title ->
                val chapterId = dao.insertStudyChapter(StudyChapter(materialId = materialId, title = title, orderIndex = index))
                val taskId = dao.insert(
                    StudyTask(
                        subject = fileName.substringBeforeLast('.').ifBlank { "Imported course" },
                        topic = title,
                        examDay = examDay,
                        totalMinutes = minutesPerChapter,
                        priority = 2,
                        chapterId = chapterId
                    )
                )
                if (index == 0) {
                    firstTaskId = taskId
                    firstChapterId = chapterId
                }
                val seeds = CourseTemplates.questionsFor(title)
                if (seeds.isNotEmpty()) {
                    dao.insertQuizQuestions(seeds.map { seed ->
                        QuizQuestion(
                            chapterId = chapterId,
                            prompt = seed.prompt,
                            options = QuizQuestion.encodeOptions(seed.options),
                            correctIndex = seed.correctIndex,
                            explanation = seed.explanation
                        )
                    })
                }
            }
            dao.clearPending(today.toEpochDay())
            dao.bumpRevision()
            ImportedCourse(materialId, firstTaskId, firstChapterId)
        }
    }

    suspend fun addStudentQuestion(
        chapterId: Long,
        prompt: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String
    ) {
        require(prompt.isNotBlank()) { "Enter a question." }
        require(options.size == 4 && options.all { it.isNotBlank() }) { "Enter four answer choices." }
        require(correctIndex in options.indices) { "Choose the correct answer." }
        database.withTransaction {
            dao.initialize(Revision())
            require(dao.studyChapters().any { it.id == chapterId }) { "This chapter is no longer available." }
            dao.insertQuizQuestions(
                listOf(
                    QuizQuestion(
                        chapterId = chapterId,
                        prompt = prompt.trim(),
                        options = QuizQuestion.encodeOptions(options.map { it.trim() }),
                        correctIndex = correctIndex,
                        explanation = explanation.trim().ifBlank { "Review the course material for this answer." },
                        source = QuizQuestion.SOURCE_STUDENT
                    )
                )
            )
            dao.bumpRevision()
        }
    }

    /** Caches validated Gemini questions so later sessions can quiz offline. */
    suspend fun saveGeneratedQuestions(chapterId: Long, questions: List<GeneratedQuizQuestion>) {
        require(questions.isNotEmpty()) { "Gemini did not return usable questions." }
        database.withTransaction {
            dao.initialize(Revision())
            require(dao.studyChapters().any { it.id == chapterId }) { "This chapter is no longer available." }
            dao.insertQuizQuestions(questions.map { question ->
                QuizQuestion(
                    chapterId = chapterId,
                    prompt = question.prompt,
                    options = QuizQuestion.encodeOptions(question.options),
                    correctIndex = question.correctIndex,
                    explanation = question.explanation,
                    source = QuizQuestion.SOURCE_AI
                )
            })
            dao.bumpRevision()
        }
    }

    suspend fun saveQuizAttempt(chapterId: Long, sessionId: Long?, score: Int, total: Int) {
        require(total > 0 && score in 0..total) { "Invalid quiz result." }
        database.withTransaction {
            dao.initialize(Revision())
            require(dao.studyChapters().any { it.id == chapterId }) { "This chapter is no longer available." }
            if (sessionId != null) require(dao.quizAttemptForSession(sessionId) == null) { "This session quiz was already completed." }
            dao.insertQuizAttempt(
                QuizAttempt(
                    chapterId = chapterId,
                    sessionId = sessionId,
                    score = score,
                    total = total,
                    completedAt = System.currentTimeMillis()
                )
            )
            dao.bumpRevision()
        }
    }

    suspend fun deleteCourse(materialId: Long, today: LocalDate = LocalDate.now()) {
        database.withTransaction {
            dao.initialize(Revision())
            val snapshot = readSnapshot()
            val chapterIds = snapshot.chapters.filter { it.materialId == materialId }.map { it.id }.toSet()
            val linkedTaskIds = snapshot.tasks.filter { it.chapterId in chapterIds }.map { it.id }.toSet()
            require(snapshot.sessions.none { it.taskId in linkedTaskIds && it.status != StudySession.PENDING }) {
                "This course has completed study history, so it cannot be deleted. Keep it as your learning record."
            }
            dao.deleteTasksForMaterial(materialId)
            dao.deleteCourseMaterial(materialId)
            dao.clearPending(today.toEpochDay())
            dao.bumpRevision()
        }
    }

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
     * Configures legacy daily availability minutes for backward compatibility.
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
     * Adds a new free-time window for a specific calendar day after strict validation.
     */
    suspend fun addTimeWindow(day: Long, startMinute: Int, endMinute: Int, today: LocalDate = LocalDate.now(), editId: Long = 0L) {
        database.withTransaction {
            dao.initialize(Revision())
            val currentWindows = dao.timeWindowsForDay(day)
            val error = TimeWindowValidator.validateWindow(day, startMinute, endMinute, currentWindows, today, ignoreId = editId)
            require(error == null) { error ?: "Invalid time window." }

            if (editId != 0L) {
                require(currentWindows.any { it.id == editId }) { "This window changed. Refresh the calendar." }
                dao.deleteTimeWindow(editId)
            }
            dao.insertTimeWindows(listOf(TimeWindow(day = day, startMinute = startMinute, endMinute = endMinute)))

            // Synchronize legacy DailyAvailability total for backward compatibility
            val allDayWindows = dao.timeWindowsForDay(day)
            val totalMins = allDayWindows.sumOf { it.durationMinutes }
            dao.saveAvailability(DailyAvailability(day, totalMins))

            dao.clearPending(today.toEpochDay())
            dao.bumpRevision()
        }
    }

    /**
     * Deletes a free-time window by ID.
     */
    suspend fun deleteTimeWindow(id: Long, today: LocalDate = LocalDate.now()) {
        database.withTransaction {
            dao.initialize(Revision())
            val all = dao.timeWindows()
            val target = all.find { it.id == id }
            if (target != null) {
                require(target.day >= today.toEpochDay()) { "Past dates are read-only." }
                dao.deleteTimeWindow(id)
                val remainingWindows = dao.timeWindowsForDay(target.day)
                val totalMins = remainingWindows.sumOf { it.durationMinutes }
                dao.saveAvailability(DailyAvailability(target.day, totalMins))
                dao.clearPending(today.toEpochDay())
                dao.bumpRevision()
            }
        }
    }

    /**
     * Clears all free-time windows for a given day (marks day as Day Off).
     */
    suspend fun clearTimeWindowsForDay(day: Long, today: LocalDate = LocalDate.now()) {
        val todayDay = today.toEpochDay()
        require(day >= todayDay) { "Cannot clear past dates." }

        database.withTransaction {
            dao.initialize(Revision())
            dao.clearTimeWindowsForDay(day)
            dao.saveAvailability(DailyAvailability(day, 0))
            dao.clearPending(todayDay)
            dao.bumpRevision()
        }
    }

    /**
     * Copies all configured time windows from [sourceDay] to a list of [targetDays].
     */
    suspend fun copyWindowsToDates(sourceDay: Long, targetDays: List<Long>, today: LocalDate = LocalDate.now()) {
        val todayDay = today.toEpochDay()
        database.withTransaction {
            dao.initialize(Revision())
            val sourceWindows = dao.timeWindowsForDay(sourceDay)
            require(sourceWindows.isNotEmpty()) { "Source day has no time windows to copy." }

            require(targetDays.isNotEmpty()) { "Select at least one date." }
            require(targetDays.all { it >= todayDay && it != sourceDay }) { "Choose future target dates, excluding the source." }
            // Copy is additive and atomic: never silently overwrite a different day's windows.
            for (targetDay in targetDays.distinct()) {
                val existing = dao.timeWindowsForDay(targetDay)
                val copies = sourceWindows.map { TimeWindow(day = targetDay, startMinute = it.startMinute, endMinute = it.endMinute) }
                for (copy in copies) {
                    val error = TimeWindowValidator.validateWindow(targetDay, copy.startMinute, copy.endMinute, existing, today)
                    require(error == null) { "${dateLabel(targetDay)}: $error Nothing was copied." }
                }
                dao.insertTimeWindows(copies)
                dao.saveAvailability(DailyAvailability(targetDay, (existing + copies).sumOf { it.durationMinutes }))
            }

            dao.clearPending(todayDay)
            dao.bumpRevision()
        }
    }

    /**
     * Updates local student profile preferences in Room.
     */
    suspend fun updateProfile(name: String, studyGoalMinutes: Int, preferredSplit: Boolean, avatarIndex: Int) {
        require(name.isNotBlank() && name.trim().length <= 40) { "Enter a name between 1 and 40 characters." }
        require(avatarIndex in 0..3) { "Choose a valid avatar." }
        require(studyGoalMinutes in 15..1440) { "Study target must be between 15 and 1,440 minutes." }

        database.withTransaction {
            dao.initialize(Revision())
            dao.saveUserProfile(
                UserProfile(
                    id = 1,
                    name = name.trim(),
                    studyGoalMinutes = studyGoalMinutes,
                    preferredSplit = preferredSplit,
                    avatarIndex = avatarIndex
                )
            )
            dao.bumpRevision()
        }
    }

    /**
     * Records progress for a scheduled study session.
     * Can only be recorded on or after the scheduled session day.
     * A partially or fully completed session closes permanently; future recovery schedules remaining work.
     */
    suspend fun recordProgress(sessionId: Long, minutes: Int, today: LocalDate = LocalDate.now(), currentMinute: Int = if (today == LocalDate.now()) LocalTime.now().let { it.hour * 60 + it.minute } else 0) {
        val todayDay = today.toEpochDay()
        database.withTransaction {
            dao.initialize(Revision())
            val session = requireNotNull(dao.session(sessionId)) { "This session changed. Refresh the plan." }
            require(session.status == StudySession.PENDING) { "Progress for this session is already recorded." }
            require(session.day <= todayDay) { "Record progress on the study day or afterwards." }
            require(session.day < todayDay || session.startMinute < 0 || session.startMinute <= currentMinute) { "This session has not started yet." }

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
     * Completes any pending session immediately, including work planned for later.
     * The future allocation is moved to today as an unslotted session before it is closed,
     * so progress is never falsely recorded before its scheduled start time.
     */
    suspend fun completeNow(sessionId: Long, minutes: Int, today: LocalDate = LocalDate.now()) {
        val todayDay = today.toEpochDay()
        database.withTransaction {
            dao.initialize(Revision())
            val session = requireNotNull(dao.session(sessionId)) { "This session changed. Refresh the plan." }
            require(session.status == StudySession.PENDING) { "Progress for this session is already recorded." }
            val task = requireNotNull(dao.task(session.taskId)) { "Task no longer exists." }
            val remaining = readSnapshot().remaining(task)
            require(minutes in 1..minOf(session.allocatedMinutes, remaining)) { "Enter valid completed minutes for this session." }

            dao.updateSession(
                session.copy(
                    day = todayDay,
                    startMinute = -1,
                    endMinute = -1,
                    completedMinutes = minutes,
                    status = StudySession.CLOSED
                )
            )
            dao.clearTaskPending(task.id, todayDay)
            dao.bumpRevision()
        }
    }

    /**
     * Atomically commits an accepted recovery plan to the database.
     * Strictly verifies that the database has not changed since the preview was generated.
     */
    suspend fun accept(plan: RecoveryPlan, today: LocalDate = LocalDate.now(), currentMinute: Int = if (today == LocalDate.now()) LocalTime.now().let { it.hour * 60 + it.minute } else 0) {
        val todayDay = today.toEpochDay()
        database.withTransaction {
            dao.initialize(Revision())
            require(plan.startDay == todayDay) { "The date changed. Generate a fresh preview." }
            require((dao.revision() ?: 0) == plan.revision) { "Your tasks or availability changed. Generate a new preview." }

            require(plan.sessions.none { it.day == todayDay && it.hasExactTimes && it.startMinute < currentMinute }) { "A proposed start time has passed. Recalculate your preview." }
            val expected = Planner.build(readSnapshot(), today, plan.splitTasks, plan.generatedMinute)
            require(plan == expected) { "This preview is out of date. Generate it again." }

            dao.clearPending(todayDay)
            dao.insertSessions(
                plan.sessions.map {
                    StudySession(
                        taskId = it.taskId,
                        day = it.day,
                        allocatedMinutes = it.minutes,
                        startMinute = it.startMinute,
                        endMinute = it.endMinute
                    )
                }
            )
            dao.bumpRevision()
        }
    }
}
