package com.baoji.feature.importer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.core.FileIo
import com.baoji.core.reasonForUser
import com.baoji.data.io.ImportIssue
import com.baoji.data.io.ImportKind
import com.baoji.data.io.ImportMode
import com.baoji.data.io.ImportValidator
import com.baoji.data.io.ParsedDiet
import com.baoji.data.io.ParsedTraining
import com.baoji.data.io.PlanImporter
import com.baoji.data.io.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Validation outcome for the current text. */
sealed interface Check {
    data object Empty : Check
    data class Problems(val issues: List<ImportIssue>) : Check
    data class Training(val data: ParsedTraining) : Check
    data class Diet(val data: ParsedDiet) : Check
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importer: PlanImporter,
    private val files: FileIo,
) : ViewModel() {

    private val kind = MutableStateFlow(ImportKind.TRAINING)

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    @OptIn(FlowPreview::class)
    val check: StateFlow<Check> = combine(_text.debounce(250), kind) { t, k -> validate(k, t) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Check.Empty)

    fun setKind(value: ImportKind) {
        kind.value = value
    }

    fun setText(value: String) {
        _text.value = value
        _status.value = null
    }

    fun loadFile(uri: Uri) = viewModelScope.launch {
        try {
            setText(files.readText(uri))
        } catch (e: Exception) {
            _status.value = "读取失败：${e.reasonForUser()}"
        }
    }

    fun commit(mode: ImportMode) = viewModelScope.launch {
        val current = validate(kind.value, _text.value)
        try {
            when (current) {
                is Check.Training -> importer.importTraining(current.data, mode)
                is Check.Diet -> importer.importDiet(current.data, mode)
                else -> return@launch
            }
            _done.value = true
        } catch (e: Exception) {
            _status.value = "导入失败，已全部回滚：${e.reasonForUser()}"
        }
    }

    companion object {
        fun validate(kind: ImportKind, text: String): Check {
            if (text.isBlank()) return Check.Empty
            return when (kind) {
                ImportKind.DIET -> when (val r = ImportValidator.validateDiet(text)) {
                    is ValidationResult.Ok -> Check.Diet(r.value)
                    is ValidationResult.Invalid -> Check.Problems(r.issues)
                }
                else -> when (val r = ImportValidator.validateTraining(text)) {
                    is ValidationResult.Ok -> Check.Training(r.value)
                    is ValidationResult.Invalid -> Check.Problems(r.issues)
                }
            }
        }
    }
}
