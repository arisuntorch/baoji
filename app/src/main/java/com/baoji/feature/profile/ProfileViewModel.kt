package com.baoji.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.core.FileIo
import com.baoji.core.reasonForUser
import com.baoji.data.io.BackupDto
import com.baoji.data.io.BackupRestorer
import com.baoji.data.io.ValidationResult
import com.baoji.data.prefs.NutritionTargets
import com.baoji.data.prefs.SettingsStore
import com.baoji.data.prefs.ThemeMode
import com.baoji.data.prefs.VibrationLevel
import com.baoji.data.repo.JournalRepository
import com.baoji.data.repo.Profile
import com.baoji.data.repo.ProfileRepository
import com.baoji.domain.Sex
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed interface DataDialog {
    data class ConfirmRestore(val backup: BackupDto) : DataDialog
    data object ConfirmClear : DataDialog
    data object ConfirmClearAgain : DataDialog
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val journal: JournalRepository,
    private val settings: SettingsStore,
    private val restorer: BackupRestorer,
    private val files: FileIo,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val profile: StateFlow<Profile?> = profiles.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val targets: StateFlow<NutritionTargets> = settings.targets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionTargets.EMPTY)
    val vibration: StateFlow<VibrationLevel> = settings.vibration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VibrationLevel.STANDARD)
    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    private val _dialog = MutableStateFlow<DataDialog?>(null)
    val dialog: StateFlow<DataDialog?> = _dialog.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _cleared = MutableStateFlow(false)
    val cleared: StateFlow<Boolean> = _cleared.asStateFlow()

    fun setHeight(text: String) = text.toDoubleOrNull()?.takeIf { it in 100.0..250.0 }?.let {
        viewModelScope.launch { profiles.updateHeight(it) }
    }

    fun setWeight(text: String) = text.toDoubleOrNull()
        ?.takeIf { it in JournalRepository.MIN_KG..JournalRepository.MAX_KG }
        ?.let { viewModelScope.launch { journal.saveWeight(it) } }

    fun setAge(text: String) = text.toIntOrNull()?.takeIf { it in 10..100 }?.let {
        viewModelScope.launch { profiles.updateAge(it) }
    }

    fun setSex(sex: Sex) = viewModelScope.launch { profiles.updateSex(sex) }

    fun setTargets(protein: String, carbs: String, fat: String) {
        val p = protein.toDoubleOrNull() ?: return
        val c = carbs.toDoubleOrNull() ?: return
        val f = fat.toDoubleOrNull() ?: return
        if (p < 0 || c < 0 || f < 0) return
        viewModelScope.launch { settings.setTargets(NutritionTargets(p, c, f)) }
    }

    fun setVibration(level: VibrationLevel) = viewModelScope.launch { settings.setVibration(level) }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }

    /** Copies the picked image into private storage so it survives the source being deleted. */
    fun setAvatar(uri: Uri) = viewModelScope.launch {
        try {
            val target = withContext(Dispatchers.IO) {
                val out = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
                context.filesDir.listFiles { f -> f.name.startsWith("avatar_") && f != out }?.forEach { it.delete() }
                out
            }
            profiles.updateAvatar(target.absolutePath)
        } catch (e: Exception) {
            _message.value = "头像保存失败"
        }
    }

    fun pickBackup(uri: Uri) = viewModelScope.launch {
        val text = try {
            files.readText(uri)
        } catch (e: Exception) {
            _message.value = "读取失败：${e.reasonForUser()}"
            return@launch
        }
        when (val r = withContext(Dispatchers.Default) { restorer.parse(text) }) {
            is ValidationResult.Ok -> _dialog.value = DataDialog.ConfirmRestore(r.value)
            is ValidationResult.Invalid -> _message.value = r.issues.joinToString("\n") { "第 ${it.line} 行 ${it.message}" }
        }
    }

    fun confirmRestore(backup: BackupDto) = viewModelScope.launch {
        _dialog.value = null
        _message.value = try {
            restorer.restore(backup)
            "已恢复"
        } catch (e: Exception) {
            "恢复失败，原数据未改动：${e.reasonForUser()}"
        }
    }

    fun askClear() {
        _dialog.value = DataDialog.ConfirmClear
    }

    fun confirmClearOnce() {
        _dialog.value = DataDialog.ConfirmClearAgain
    }

    fun confirmClearFinal() = viewModelScope.launch {
        _dialog.value = null
        restorer.clearAll()
        withContext(Dispatchers.IO) {
            context.filesDir.listFiles { f -> f.name.startsWith("avatar_") }?.forEach { it.delete() }
        }
        _cleared.value = true
    }

    fun dismissDialog() {
        _dialog.value = null
    }

    fun clearMessage() {
        _message.value = null
    }
}
