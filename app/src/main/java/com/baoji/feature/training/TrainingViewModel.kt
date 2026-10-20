package com.baoji.feature.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.core.HapticKind
import com.baoji.core.Haptics
import com.baoji.core.time.TimeSource
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

    private val editing = MutableStateFlow<String?>(null)
    val editingId: StateFlow<String?> = editing

    fun checkIn() = viewModelScope.launch {
        if (repo.checkIn()) haptics.play(HapticKind.HEAVY)
    }

    fun completeSet(exerciseId: String) = viewModelScope.launch {
        when (repo.completeSet(exerciseId)) {
            SetOutcome.SET_DONE -> haptics.play(HapticKind.LIGHT)
            SetOutcome.EXERCISE_DONE -> haptics.play(HapticKind.MEDIUM)
            SetOutcome.DAY_DONE -> haptics.play(HapticKind.DAY_COMPLETE)
            SetOutcome.NOTHING -> Unit
        }
    }

    fun undoSet(exerciseId: String) = viewModelScope.launch {
        if (repo.undoSet(exerciseId)) haptics.play(HapticKind.LIGHT)
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

    fun history(name: String?): Flow<List<DayMax>> = if (name == null) emptyFlow() else repo.observeHistory(name)

    fun addExtra(text: String) = viewModelScope.launch {
        journal.addFreeLog(FreeLogType.EXTRA_TRAINING, text)
    }
}
