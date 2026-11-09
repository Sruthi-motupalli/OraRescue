package com.ora.rescue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Custom Application class providing dependency injection for the singleton repository.
 */
class OraApplication : Application() {
    val repository by lazy { StudyRepository(StudyDatabase.create(this)) }
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

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _preview = MutableStateFlow<RecoveryPlan?>(null)
    val preview: StateFlow<RecoveryPlan?> = _preview.asStateFlow()

    private val notices = Channel<UiNotice>(Channel.BUFFERED)
    val events: Flow<UiNotice> = notices.receiveAsFlow()

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

    /** Sets daily study availability in minutes. */
    fun setAvailability(day: Long, minutes: Int) = perform(
        message = "Availability saved. Generate a fresh recovery plan."
    ) {
        repository.availability(day, minutes)
    }

    /** Records completed study progress for a session. */
    fun progress(id: Long, minutes: Int) = perform(
        message = "Progress saved. Recover remaining work when you’re ready."
    ) {
        repository.recordProgress(id, minutes)
    }

    /** Generates an in-memory preview of the recovery plan for the user to review. */
    fun preview(split: Boolean) = perform(message = null) {
        _preview.value = Planner.build(repository.snapshot(), LocalDate.now(), split)
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
