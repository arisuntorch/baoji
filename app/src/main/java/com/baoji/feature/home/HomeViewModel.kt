package com.baoji.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.core.time.TimeSource
import com.baoji.data.repo.EnergyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeState(val intake: Long, val burned: Long?, val isTrainingDay: Boolean)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val energy: EnergyRepository,
    private val time: TimeSource,
) : ViewModel() {

    /** Ticks once per second, aligned to the wall clock; stops when the screen isn't visible. */
    private val ticks = flow {
        while (true) {
            emit(Unit)
            delay(1_000 - time.nowMillis() % 1_000)
        }
    }

    private val days = time.days().shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val state: StateFlow<HomeState?> = combine(energy.observeDay(days), ticks) { day, _ ->
        HomeState(
            intake = Math.round(day.intakeKcal),
            burned = energy.burnedNow(day)?.let { Math.round(it) },
            isTrainingDay = day.isTrainingDay,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), null)
}
