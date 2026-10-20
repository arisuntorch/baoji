package com.baoji.feature.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.core.HapticKind
import com.baoji.core.Haptics
import com.baoji.core.time.TimeSource
import com.baoji.data.db.FreeLogEntity
import com.baoji.data.db.FreeLogType
import com.baoji.data.repo.DietDay
import com.baoji.data.repo.DietRepository
import com.baoji.data.repo.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DietViewModel @Inject constructor(
    private val repo: DietRepository,
    private val journal: JournalRepository,
    private val haptics: Haptics,
    time: TimeSource,
) : ViewModel() {

    private val days = time.days().shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val day: StateFlow<DietDay?> = repo.observeDay(days)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val snacks: StateFlow<List<FreeLogEntity>> = days
        .flatMapLatest { journal.observeFreeLogs(FreeLogType.EXTRA_MEAL, it.toEpochDay()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(taskId: String) = viewModelScope.launch {
        if (repo.toggle(taskId)) haptics.play(HapticKind.LIGHT)
    }

    fun addSnack(text: String) = viewModelScope.launch {
        journal.addFreeLog(FreeLogType.EXTRA_MEAL, text)
    }
}
