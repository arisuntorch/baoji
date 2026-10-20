package com.baoji.feature.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.core.time.TimeSource
import com.baoji.data.repo.DayMark
import com.baoji.data.repo.DayPreview
import com.baoji.data.repo.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Read-only month calendar opened from the training page's date. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repo: TrainingRepository,
    private val time: TimeSource,
) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.from(time.today()))
    val month: StateFlow<YearMonth> = _month

    private val _selected = MutableStateFlow(time.today())
    val selected: StateFlow<LocalDate> = _selected

    val marks: StateFlow<Map<LocalDate, DayMark>> = _month
        .flatMapLatest { repo.observeMonthMarks(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val preview: StateFlow<DayPreview?> = _selected
        .flatMapLatest { repo.observePreview(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun today(): LocalDate = time.today()

    fun shiftMonth(by: Long) {
        _month.value = _month.value.plusMonths(by)
    }

    fun select(date: LocalDate) {
        _selected.value = date
        _month.value = YearMonth.from(date)
    }

    /** Back to today, e.g. each time the sheet opens. */
    fun reset() = select(time.today())
}
