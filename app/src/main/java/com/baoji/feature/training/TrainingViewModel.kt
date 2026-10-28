package com.baoji.feature.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.core.HapticKind
import com.baoji.core.Haptics
import com.baoji.core.time.TimeSource
import com.baoji.core.ui.Undo
import com.baoji.data.catalog.CatalogRepository
import com.baoji.data.catalog.ExerciseItem
import com.baoji.data.repo.ActivityDay
import com.baoji.data.repo.ActivityRepository
import com.baoji.data.db.FreeLogEntity
import com.baoji.data.db.FreeLogType
import com.baoji.data.repo.DayMax
import com.baoji.data.repo.JournalRepository
import com.baoji.data.repo.SetOutcome
import com.baoji.data.repo.TrainingDay
import com.baoji.data.repo.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val repo: TrainingRepository,
    private val journal: JournalRepository,
    private val activity: ActivityRepository,
    private val catalog: CatalogRepository,
    private val haptics: Haptics,
    time: TimeSource,
) : ViewModel() {

    private val days = time.days().shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val day: StateFlow<TrainingDay?> = repo.observeDay(days)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val extras: StateFlow<List<FreeLogEntity>> = days
        .flatMapLatest { journal.observeFreeLogs(FreeLogType.EXTRA_TRAINING, it.toEpochDay()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activityDay: StateFlow<ActivityDay> = activity.observeDay(days)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityDay(null, emptyList()))

    val weightKg: StateFlow<Double?> = activity.observeWeightKg(days)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val editing = MutableStateFlow<String?>(null)
    val editingId: StateFlow<String?> = editing

    fun completeSet(exerciseId: String) = viewModelScope.launch {
        when (repo.completeSet(exerciseId)) {
            SetOutcome.SET_DONE -> haptics.play(HapticKind.LIGHT)
            SetOutcome.EXERCISE_DONE -> haptics.play(HapticKind.MEDIUM)
            SetOutcome.DAY_DONE -> haptics.play(HapticKind.DAY_COMPLETE)
            SetOutcome.NOTHING -> Unit
        }
    }

    /** Long press: clears every set of this exercise today, with a firm buzz. */
    fun reset(exerciseId: String) = viewModelScope.launch {
        if (repo.reset(exerciseId)) haptics.play(HapticKind.HEAVY)
    }

    private val pendingUndo = MutableStateFlow<Undo?>(null)
    val undo: StateFlow<Undo?> = pendingUndo

    fun archive(exerciseId: String) = viewModelScope.launch {
        val name = day.value?.rows?.firstOrNull { it.exercise.id == exerciseId }?.exercise?.name ?: return@launch
        repo.archive(exerciseId)
        haptics.play(HapticKind.MEDIUM)
        pendingUndo.value = Undo(name) { repo.unarchive(exerciseId) }
    }

    fun undo() {
        val u = pendingUndo.value ?: return
        pendingUndo.value = null
        viewModelScope.launch { u.restore() }
    }

    fun dismissUndo(u: Undo) {
        pendingUndo.compareAndSet(u, null)
    }

    fun setSteps(steps: Int) = viewModelScope.launch {
        if (activity.setSteps(steps)) haptics.play(HapticKind.LIGHT)
    }

    fun resetSteps() = viewModelScope.launch {
        if (activityDay.value.steps != null && activity.setSteps(0)) haptics.play(HapticKind.HEAVY)
    }

    suspend fun searchExercises(query: String): List<ExerciseItem> =
        if (query.isBlank()) catalog.allExercises() else catalog.searchExercises(query)

    fun addTimed(item: ExerciseItem, minutes: Double) = viewModelScope.launch {
        if (activity.addTimed(item, minutes)) haptics.play(HapticKind.LIGHT)
    }

    fun deleteActivity(id: String) = viewModelScope.launch {
        val log = activityDay.value.timed.firstOrNull { it.id == id } ?: return@launch
        if (activity.delete(id)) {
            haptics.play(HapticKind.MEDIUM)
            pendingUndo.value = Undo(log.name) { activity.restore(log) }
        }
    }

    fun openEditor(exerciseId: String) {
        editing.value = exerciseId
    }

    fun closeEditor() {
        editing.value = null
    }

    fun adjust(exerciseId: String, weight: Double, reps: Int) = viewModelScope.launch {
        repo.adjust(exerciseId, weight, reps)
    }

    fun setSets(exerciseId: String, sets: Int) = viewModelScope.launch {
        repo.setSets(exerciseId, sets)
    }

    fun history(name: String?): Flow<List<DayMax>> = if (name == null) emptyFlow() else repo.observeHistory(name)

    fun addExtra(text: String) = viewModelScope.launch {
        journal.addFreeLog(FreeLogType.EXTRA_TRAINING, text)
    }
}
