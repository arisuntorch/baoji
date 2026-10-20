package com.baoji.feature.home

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.core.FileIo
import com.baoji.core.reasonForUser
import com.baoji.core.time.TimeSource
import com.baoji.data.db.ExportDao
import com.baoji.data.io.ExportFile
import com.baoji.data.io.ExportRange
import com.baoji.data.io.Exporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class ExportUi(
    val busy: Boolean = false,
    val ready: ExportFile? = null,
    val range: ExportRange? = null,
    val message: String? = null,
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exporter: Exporter,
    private val files: FileIo,
    private val time: TimeSource,
    exportDao: ExportDao,
) : ViewModel() {

    val lastExportDate: StateFlow<LocalDate?> = exportDao.observeLast()
        .map { r -> r?.let { Instant.ofEpochMilli(it.timestamp).atZone(time.now().zone).toLocalDate() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow(ExportUi())
    val ui: StateFlow<ExportUi> = _ui.asStateFlow()

    fun prepare(range: ExportRange) = viewModelScope.launch {
        _ui.value = ExportUi(busy = true)
        _ui.value = try {
            ExportUi(ready = exporter.export(range), range = range)
        } catch (e: Exception) {
            ExportUi(message = "导出失败：${e.reasonForUser()}")
        }
    }

    fun saveTo(uri: Uri) = viewModelScope.launch {
        val s = _ui.value
        val file = s.ready ?: return@launch
        try {
            files.writeText(uri, file.json)
            s.range?.let { exporter.record(it) }
            _ui.update { it.copy(message = "已保存") }
        } catch (e: Exception) {
            // Some providers (e.g. emulator shared folders) hand back a URI we cannot write; say what to do instead.
            _ui.update { it.copy(message = "保存失败：${e.reasonForUser()}。换个文件夹，或用分享。") }
        }
    }

    /** Sharing counts as exported once the chooser opens; we cannot see what the target does. */
    suspend fun shareIntent(): Intent? {
        val s = _ui.value
        val file = s.ready ?: return null
        return try {
            files.shareIntent(file.fileName, file.json).also { s.range?.let { exporter.record(it) } }
        } catch (e: Exception) {
            _ui.update { it.copy(message = "分享失败：${e.reasonForUser()}") }
            null
        }
    }

    fun reset() {
        _ui.value = ExportUi()
    }
}
