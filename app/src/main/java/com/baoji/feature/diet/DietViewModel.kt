package com.baoji.feature.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.core.HapticKind
import com.baoji.core.Haptics
import com.baoji.core.time.TimeSource
import com.baoji.core.ui.Undo
import com.baoji.data.catalog.CatalogRepository
import com.baoji.data.catalog.FoodItem
import com.baoji.data.db.FreeLogEntity
import com.baoji.data.db.FreeLogType
import com.baoji.data.repo.DietDay
import com.baoji.data.repo.DietRepository
import com.baoji.data.repo.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val catalog: CatalogRepository,
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

    /** Long press: clears today's check with a firm buzz so a mis-tap is easy to undo. */
    fun reset(taskId: String) = viewModelScope.launch {
        if (repo.reset(taskId)) haptics.play(HapticKind.HEAVY)
    }

    private val pendingUndo = MutableStateFlow<Undo?>(null)
    val undo: StateFlow<Undo?> = pendingUndo

    fun archive(taskId: String) = viewModelScope.launch {
        val name = day.value?.rows?.firstOrNull { it.task.id == taskId }?.task?.name ?: return@launch
        repo.archive(taskId)
        haptics.play(HapticKind.MEDIUM)
        pendingUndo.value = Undo(name) { repo.unarchive(taskId) }
    }

    fun undo() {
        val u = pendingUndo.value ?: return
        pendingUndo.value = null
        viewModelScope.launch { u.restore() }
    }

    fun dismissUndo(u: Undo) {
        pendingUndo.compareAndSet(u, null)
    }

    suspend fun searchFoods(query: String): List<FoodItem> = catalog.searchFoods(query)

    fun addFood(food: FoodItem, grams: Double) = viewModelScope.launch {
        repo.addFood(food, grams)
        haptics.play(HapticKind.LIGHT)
    }

    fun deleteExtra(logId: String) = viewModelScope.launch {
        val log = day.value?.extras?.firstOrNull { it.id == logId } ?: return@launch
        if (repo.deleteExtra(logId)) {
            haptics.play(HapticKind.MEDIUM)
            pendingUndo.value = Undo(log.name) { repo.restoreExtra(log) }
        }
    }
}
