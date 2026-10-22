package com.baoji.feature.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.SecondaryButton
import com.baoji.core.ui.SolidButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(onDismiss: () -> Unit, vm: UpdateViewModel = hiltViewModel()) {
    val c = Baoji.colors
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launch = { intent: Intent? ->
        try {
            intent?.let(context::startActivity)
        } catch (e: ActivityNotFoundException) {
            vm.installUnavailable()
        }
    }

    LaunchedEffect(Unit) { vm.check() }
    DisposableEffect(Unit) { onDispose { vm.cancel() } }
    // Open the system installer once, as soon as the download is done.
    val readyApk = (ui as? UpdateUi.Ready)?.apk
    LaunchedEffect(readyApk) { if (readyApk != null) launch(vm.autoInstallIntent()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.background,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding).navigationBarsPadding()) {
            Text("检查更新", style = MaterialTheme.typography.titleLarge, color = c.primaryText)
            Text("当前版本 ${vm.currentVersion}", style = MaterialTheme.typography.bodySmall, color = c.secondaryText)
            Spacer(Modifier.height(16.dp))
            when (val s = ui) {
                UpdateUi.Checking -> Status("正在检查…")
                is UpdateUi.UpToDate -> Status("已经是最新版本")
                is UpdateUi.Failed -> {
                    Status(s.message)
                    Spacer(Modifier.height(16.dp))
                    SecondaryButton("重试", Modifier.fillMaxWidth()) { vm.check() }
                }
                is UpdateUi.Downloading -> {
                    Status("发现新版本 ${s.info.versionName}，正在下载 ${(s.progress * 100).toInt()}%")
                    Spacer(Modifier.height(10.dp))
                    ProgressLine(s.progress)
                    Notes(s.info.notes)
                }
                is UpdateUi.Ready -> {
                    Status(
                        if (s.needsPermission) "在系统设置里允许「薄肌」安装应用，回来再点安装"
                        else "新版本 ${s.info.versionName} 已下载，数据会保留",
                    )
                    Spacer(Modifier.height(16.dp))
                    SolidButton("安装", Modifier.fillMaxWidth()) { launch(vm.installIntent()) }
                    Notes(s.info.notes)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Status(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = Baoji.colors.primaryText)
}

@Composable
private fun ProgressLine(progress: Float) {
    val c = Baoji.colors
    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(c.fill)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).background(c.primaryText))
    }
}

@Composable
private fun Notes(notes: String) {
    if (notes.isBlank()) return
    Spacer(Modifier.height(16.dp))
    Text(
        notes,
        style = MaterialTheme.typography.bodySmall,
        color = Baoji.colors.secondaryText,
        modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
    )
}
