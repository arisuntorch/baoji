package com.baoji.feature.profile

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baoji.data.update.ReleaseInfo
import com.baoji.data.update.UpdateProblem
import com.baoji.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject

sealed interface UpdateUi {
    data object Checking : UpdateUi
    data class UpToDate(val version: String) : UpdateUi
    data class Downloading(val info: ReleaseInfo, val progress: Float) : UpdateUi
    data class Ready(val info: ReleaseInfo, val apk: File, val needsPermission: Boolean = false) : UpdateUi
    data class Failed(val message: String) : UpdateUi
}

@HiltViewModel
class UpdateViewModel @Inject constructor(private val repo: UpdateRepository) : ViewModel() {

    private val _ui = MutableStateFlow<UpdateUi>(UpdateUi.Checking)
    val ui: StateFlow<UpdateUi> = _ui.asStateFlow()

    val currentVersion: String get() = repo.currentVersion

    private var job: Job? = null

    /** The installer opens by itself once per finished download, not again on every recomposition or reopen. */
    private var autoInstalled: File? = null

    /** Check, and if a newer release exists download it straight away. */
    fun check() {
        if (job?.isActive == true) return
        // Reopening the sheet after a finished download keeps it instead of downloading again.
        if ((_ui.value as? UpdateUi.Ready)?.apk?.exists() == true) return
        // Lazy start: viewModelScope runs immediately, and the body must see `job` already pointing at itself.
        val run = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val self = coroutineContext.job
            // A cancelled run may still be blocked in a network read; its late updates must not reach the screen.
            fun publish(ui: UpdateUi) { if (job === self) _ui.value = ui }
            publish(UpdateUi.Checking)
            try {
                val info = repo.checkNewer()
                if (info == null) {
                    publish(UpdateUi.UpToDate(repo.currentVersion))
                    return@launch
                }
                publish(UpdateUi.Downloading(info, 0f))
                val apk = repo.download(info) { p ->
                    if (job === self) _ui.update { if (it is UpdateUi.Downloading) it.copy(progress = p) else it }
                }
                publish(UpdateUi.Ready(info, apk))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                publish(UpdateUi.Failed(e.updateReason()))
            }
        }
        job = run
        run.start()
    }

    /** The intent to launch for the install button: the installer, or the "allow installs" switch the first time. */
    fun installIntent(): Intent? {
        val ready = _ui.value as? UpdateUi.Ready ?: return null
        if (repo.canInstall()) return repo.installIntent(ready.apk)
        _ui.value = ready.copy(needsPermission = true)
        return repo.installPermissionIntent()
    }

    /** [installIntent] for the automatic launch right after a download; null if it already happened for this file. */
    fun autoInstallIntent(): Intent? {
        val apk = (_ui.value as? UpdateUi.Ready)?.apk ?: return null
        if (autoInstalled == apk) return null
        autoInstalled = apk
        return installIntent()
    }

    /** Some devices block app installs entirely and have no screen to allow them. */
    fun installUnavailable() {
        _ui.value = UpdateUi.Failed("这台设备不允许应用自行安装，请到 Releases 页面手动下载")
    }

    fun cancel() {
        job?.cancel()
    }
}

private fun Exception.updateReason(): String = when (this) {
    is UpdateProblem -> message.orEmpty()
    is IOException -> "连不上 GitHub，检查一下网络"
    else -> "未知错误"
}
