package com.baoji.feature.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.core.AppScope
import com.baoji.core.time.TimeSource
import com.baoji.data.repo.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DiaryListItem(val date: LocalDate, val preview: String)

@HiltViewModel
class DiaryListViewModel @Inject constructor(
    journal: JournalRepository,
    private val time: TimeSource,
) : ViewModel() {

    /** Today always sits on top, even before anything is written. */
    val items: StateFlow<List<DiaryListItem>> = journal.observeDiaries()
        .map { rows ->
            val today = time.today()
            val written = rows.filter { it.content.isNotBlank() }
                .map { DiaryListItem(LocalDate.ofEpochDay(it.date), it.content.lineSequence().first().take(60)) }
            val todayItem = written.firstOrNull { it.date == today } ?: DiaryListItem(today, "")
            listOf(todayItem) + written.filter { it.date != today }.sortedByDescending { it.date }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@HiltViewModel
class DiaryEditViewModel @Inject constructor(
    private val journal: JournalRepository,
    private val appScope: AppScope,
    handle: SavedStateHandle,
) : ViewModel() {

    val epochDay: Long = handle.get<Long>("date") ?: 0L

    private val _text = MutableStateFlow<String?>(null)
    /** null until the stored entry has loaded, so the editor never flashes empty over real text. */
    val text: StateFlow<String?> = _text.asStateFlow()

    init {
        viewModelScope.launch {
            _text.value = journal.diary(epochDay)?.content.orEmpty()
            autosave()
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun autosave() {
        _text.drop(1).debounce(600).collect { value -> value?.let { journal.saveDiary(epochDay, it) } }
    }

    fun setText(value: String) {
        _text.value = value
    }

    /** Flush on leave so the last keystrokes are never lost. */
    override fun onCleared() {
        val value = _text.value ?: return
        appScope.launch { journal.saveDiary(epochDay, value) }
    }
}
