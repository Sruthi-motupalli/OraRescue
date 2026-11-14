package com.ora.rescue

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Custom Application class providing dependency injection for the singleton repository.
 */
class OraApplication : Application() {
    val repository by lazy { StudyRepository(StudyDatabase.create(this)) }
    val prefs by lazy { getSharedPreferences("ora_prefs", Context.MODE_PRIVATE) }
}

/** Immutable UI state representing the current snapshot, loading status, or error. */
data class StudyUiState(
    val snapshot: StudySnapshot = StudySnapshot(),
    val loading: Boolean = true,
    val error: String? = null
)

/** Transient UI event representing a snackbar message and optional navigation destination. */
data class UiNotice(val message: String, val destination: String? = null)

/**
 * ViewModel managing UI state and user actions across all Ora Rescue screens.
 * Exposes lifecycle-safe StateFlow and buffered Channel events.
 */
class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as OraApplication).repository
    private val prefs      = (application as OraApplication).prefs
    private val quizGenerator = GeminiQuizGenerator(application.contentResolver)

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _preview = MutableStateFlow<RecoveryPlan?>(null)
    val preview: StateFlow<RecoveryPlan?> = _preview.asStateFlow()

    private val _selectedChapterId = MutableStateFlow(prefs.getLong("selected_chapter_id", 0L).takeIf { it > 0 })
    val selectedChapterId: StateFlow<Long?> = _selectedChapterId.asStateFlow()

    private val notices = Channel<UiNotice>(Channel.BUFFERED)
    val events: Flow<UiNotice> = notices.receiveAsFlow()

    // ─── Theme Preference ─────────────────────────────────────────────────────
    private val _themePreference = MutableStateFlow(loadThemePreference())
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    private fun loadThemePreference(): ThemePreference {
        val stored = prefs.getString("theme", ThemePreference.SYSTEM.name) ?: ThemePreference.SYSTEM.name
        return runCatching { ThemePreference.valueOf(stored) }.getOrDefault(ThemePreference.SYSTEM)
    }

    /** Updates the theme preference and persists it across restarts. */
    fun setThemePreference(pref: ThemePreference) {
        _themePreference.value = pref
        prefs.edit().putString("theme", pref.name).apply()
    }

    init {
        reload()
    }

    /** Observes live repository snapshots and updates UI state. */
    fun reload() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                repository.snapshots.collect { snapshot ->
                    _state.value = StudyUiState(snapshot, loading = false)
                    // If the database revision changed, invalidate any active preview
                    if (_preview.value?.revision != snapshot.revision) {
                        _preview.value = null
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _state.value = _state.value.copy(
                    loading = false,
                    error = "Could not load your study data. Please try again."
                )
            }
        }
    }

    /** Helper executing a mutating repository action with busy indicator and error handling. */
    private fun perform(message: String?, destination: String? = null, block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                block()
                if (message != null) notices.send(UiNotice(message, destination))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                notices.send(UiNotice(error.message ?: "Something went wrong. Please try again."))
            } finally {
                _busy.value = false
            }
        }
    }

    /** Saves a new or edited topic. */
    fun save(task: StudyTask) = perform(
        message = "Topic saved. Preview a recovery plan to update your schedule.",
        destination = "home"
    ) {
        repository.saveTask(task)
    }

    /** Deletes an existing topic and its associated sessions. */
    fun delete(id: Long) = perform(
        message = "Topic deleted. Preview your updated plan.",
        destination = "home"
    ) {
        repository.deleteTask(id)
    }

    /** Sets daily study availability in minutes (legacy). */
    fun setAvailability(day: Long, minutes: Int) = perform(
        message = "Availability saved. Generate a fresh recovery plan."
    ) {
        repository.availability(day, minutes)
    }

    /** Adds a new free-time window for a specific date. */
    fun addTimeWindow(day: Long, startMinute: Int, endMinute: Int) = perform(
        message = "Free-time window added (${TimeWindow.formatMinuteOfDay(startMinute)} – ${TimeWindow.formatMinuteOfDay(endMinute)})."
    ) {
        repository.addTimeWindow(day, startMinute, endMinute)
    }

    fun editTimeWindow(id: Long, day: Long, start: Int, end: Int) = perform("Time window updated. Recalculate your plan.") {
        repository.addTimeWindow(day, start, end, editId = id)
    }

    /** Removes a free-time window. */
    fun deleteTimeWindow(id: Long) = perform(
        message = "Time window removed."
    ) {
        repository.deleteTimeWindow(id)
    }

    /** Clears all time windows for a day (marking it as Day Off). */
    fun clearTimeWindows(day: Long) = perform(
        message = "Marked as Day Off (0 available minutes)."
    ) {
        repository.clearTimeWindowsForDay(day)
    }

    /** Copies time windows from source day to multiple target upcoming days. */
    fun copyTimeWindows(sourceDay: Long, targetDays: List<Long>) = perform(
        message = "Time windows copied to ${targetDays.size} upcoming days."
    ) {
        repository.copyWindowsToDates(sourceDay, targetDays)
    }

    /** Updates local student profile preferences in Room. */
    fun updateProfile(name: String, studyGoalMinutes: Int, preferredSplit: Boolean, avatarIndex: Int) = perform(
        message = "Profile preferences updated."
    ) {
        repository.updateProfile(name, studyGoalMinutes, preferredSplit, avatarIndex)
    }

    /** Records completed study progress for a session. */
    fun progress(id: Long, minutes: Int) {
        val snapshot = _state.value.snapshot
        val session = snapshot.sessions.firstOrNull { it.id == id }
        val chapter = session?.let { snapshot.chapterForTask(it.taskId) }
        if (chapter == null || minutes <= 0) {
            perform("Progress saved. Recover remaining work when you’re ready.") {
                repository.recordProgress(id, minutes)
            }
            return
        }
        if (snapshot.questionsForChapter(chapter.id).isNotEmpty()) {
            perform("Session finished. Your chapter challenge is ready!", "quiz/session/$id") {
                repository.recordProgress(id, minutes)
            }
            return
        }
        val material = snapshot.courseMaterials.firstOrNull { it.id == chapter.materialId }
        perform(message = null) {
            // Completion is saved first; a network failure must never erase studied work.
            repository.recordProgress(id, minutes)
            require(material != null) { "The linked course material is no longer available." }
            val questions = quizGenerator.generate(material, chapter.title)
            repository.saveGeneratedQuestions(chapter.id, questions)
            notices.send(UiNotice("Session finished. Gemini created your chapter challenge!", "quiz/session/$id"))
        }
    }

    /** Lets a learner pull planned work forward and finish it now from the home queue. */
    fun completeNow(id: Long, minutes: Int) {
        val snapshot = _state.value.snapshot
        val session = snapshot.sessions.firstOrNull { it.id == id }
        val chapter = session?.let { snapshot.chapterForTask(it.taskId) }
        if (chapter == null) {
            perform("Session completed and removed from today's active list.") {
                repository.completeNow(id, minutes)
            }
            return
        }
        if (snapshot.questionsForChapter(chapter.id).isNotEmpty()) {
            perform("Session finished. Your chapter challenge is ready!", "quiz/session/$id") {
                repository.completeNow(id, minutes)
            }
            return
        }
        val material = snapshot.courseMaterials.firstOrNull { it.id == chapter.materialId }
        perform(message = null) {
            repository.completeNow(id, minutes)
            require(material != null) { "The linked course material is no longer available." }
            val questions = quizGenerator.generate(material, chapter.title)
            repository.saveGeneratedQuestions(chapter.id, questions)
            notices.send(UiNotice("Session finished. Gemini created your chapter challenge!", "quiz/session/$id"))
        }
    }

    fun importCourse(fileName: String, sourceUri: String, mimeType: String, examDay: Long, minutesPerChapter: Int) = perform(
        message = "Course imported. Review the generated plan, then save it.",
        destination = "recover"
    ) {
        val imported = repository.importCourse(fileName, sourceUri, mimeType, examDay, minutesPerChapter)
        _selectedChapterId.value = imported.firstChapterId
        prefs.edit().putLong("selected_chapter_id", imported.firstChapterId).apply()

        var snapshot = repository.snapshot()
        val chapter = snapshot.chapters.first { it.id == imported.firstChapterId }
        if (snapshot.questionsForChapter(chapter.id).isEmpty()) {
            val material = snapshot.courseMaterials.first { it.id == imported.materialId }
            // A network/API failure must not undo the imported study path.
            runCatching {
                val generated = quizGenerator.generate(material, chapter.title)
                repository.saveGeneratedQuestions(chapter.id, generated)
                snapshot = repository.snapshot()
            }
        }

        // Availability still controls real times; without it the preview explains the shortfall.
        val scoped = snapshot.copy(tasks = snapshot.tasks.filter { it.chapterId == null || it.chapterId == imported.firstChapterId })
        _preview.value = Planner.build(scoped, LocalDate.now(), snapshot.profile.preferredSplit, LocalTime.now().let { it.hour * 60 + it.minute })
    }

    fun addStudentQuestion(chapterId: Long, prompt: String, options: List<String>, correctIndex: Int, explanation: String) = perform(
        message = "Question added to this chapter."
    ) {
        repository.addStudentQuestion(chapterId, prompt, options, correctIndex, explanation)
    }

    fun saveQuizAttempt(chapterId: Long, sessionId: Long?, score: Int, total: Int) = perform(
        message = "Quiz result saved."
    ) {
        repository.saveQuizAttempt(chapterId, sessionId, score, total)
    }

    fun deleteCourse(materialId: Long) = perform(
        message = "Course and its linked study topics were deleted.",
        destination = "courses"
    ) {
        repository.deleteCourse(materialId)
    }

    /** Selects the one uploaded chapter that the next recovery plan should schedule. */
    fun selectStudyChapter(taskId: Long) {
        val chapterId = _state.value.snapshot.tasks.firstOrNull { it.id == taskId }?.chapterId ?: return
        _selectedChapterId.value = chapterId
        prefs.edit().putLong("selected_chapter_id", chapterId).apply()
    }

    /** Generates an in-memory preview of the recovery plan for the user to review. */
    fun preview(split: Boolean) = perform(message = null) {
        val snapshot = repository.snapshot()
        val selected = _selectedChapterId.value
        val scoped = snapshot.copy(tasks = snapshot.tasks.filter { task ->
            task.chapterId == null || task.chapterId == selected
        })
        _preview.value = Planner.build(scoped, LocalDate.now(), split, LocalTime.now().let { it.hour * 60 + it.minute })
    }

    /** Cancels and discards the active preview without modifying database state. */
    fun cancelPreview() {
        _preview.value = null
    }

    /** Accepts and commits the current preview atomically to Room. */
    fun accept() {
        val plan = _preview.value ?: return
        perform(
            message = "Your recovery plan has been applied to your study schedule.",
            destination = "home"
        ) {
            repository.accept(plan)
            _preview.value = null
        }
    }
}
