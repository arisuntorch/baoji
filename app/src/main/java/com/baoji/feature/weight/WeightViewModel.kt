package com.baoji.feature.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.data.repo.JournalRepository
import com.baoji.domain.CurvePoint
import com.baoji.domain.WeightCurve
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeightChart(val raw: List<CurvePoint>, val smooth: List<CurvePoint>) {
    val isEmpty: Boolean get() = raw.isEmpty()
}

@HiltViewModel
class WeightViewModel @Inject constructor(private val journal: JournalRepository) : ViewModel() {

    val chart: StateFlow<WeightChart> = journal.observeWeights()
        .map { logs ->
            val raw = logs.map { CurvePoint(it.date.toDouble(), it.kg) }
            WeightChart(raw, WeightCurve.monotoneSpline(WeightCurve.ema(raw)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightChart(emptyList(), emptyList()))

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setInput(v: String) {
        _input.value = v.filter { it.isDigit() || it == '.' }.take(5)
        _error.value = null
    }

    fun save() = viewModelScope.launch {
        val kg = _input.value.toDoubleOrNull()
        if (kg == null || kg !in JournalRepository.MIN_KG..JournalRepository.MAX_KG) {
            _error.value = "体重应在 20–300 kg"
            return@launch
        }
        journal.saveWeight(kg)
        _input.value = ""
    }
}
