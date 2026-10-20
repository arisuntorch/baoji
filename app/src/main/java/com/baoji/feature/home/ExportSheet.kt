package com.baoji.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.SolidButton
import com.baoji.core.ui.pressable
import com.baoji.data.io.ExportRange
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(onDismiss: () -> Unit, vm: ExportViewModel = hiltViewModel()) {
    val c = Baoji.colors
    val ui by vm.ui.collectAsStateWithLifecycle()
    val last by vm.lastExportDate.collectAsStateWithLifecycle()
    var pickingDates by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::saveTo)
    }
    DisposableEffect(Unit) { onDispose { vm.reset() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.background,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding).navigationBarsPadding()) {
            Text("导出", style = MaterialTheme.typography.titleLarge, color = c.primaryText)
            Spacer(Modifier.height(12.dp))
            val ready = ui.ready
            when {
                ready != null -> {
                    Text(ready.fileName, style = MaterialTheme.typography.bodyMedium, color = c.secondaryText)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SolidButton("保存到文件", Modifier.weight(1f)) { saver.launch(ready.fileName) }
                        SolidButton("分享", Modifier.weight(1f)) {
                            scope.launch { vm.shareIntent()?.let(context::startActivity) }
                        }
                    }
                }
                pickingDates -> DateRangeStep(onPicked = { from, to ->
                    pickingDates = false
                    vm.prepare(ExportRange.Dates(from, to))
                })
                ui.busy -> Text("正在整理…", style = MaterialTheme.typography.bodyMedium, color = c.secondaryText)
                else -> {
                    Option("全部", null) { vm.prepare(ExportRange.All) }
                    Option("自上次导出起", last?.let { "上次 ${it.monthValue}/${it.dayOfMonth}" } ?: "还没有导出过") {
                        vm.prepare(ExportRange.SinceLast)
                    }
                    Option("日期范围", null) { pickingDates = true }
                }
            }
            ui.message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.secondaryText)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Option(title: String, detail: String?, onClick: () -> Unit) {
    val c = Baoji.colors
    Column(Modifier.fillMaxWidth().pressable(label = title, onClick = onClick).padding(vertical = 14.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = c.primaryText)
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = c.secondaryText)
    }
    HorizontalDivider(thickness = 0.5.dp, color = c.separator)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeStep(onPicked: (LocalDate, LocalDate) -> Unit) {
    val c = Baoji.colors
    val state = rememberDateRangePickerState()
    val colors = DatePickerDefaults.colors(
        containerColor = c.background,
        selectedDayContainerColor = c.primaryText,
        selectedDayContentColor = c.background,
        dayInSelectionRangeContainerColor = c.fill,
        todayDateBorderColor = c.secondaryText,
        todayContentColor = c.primaryText,
    )
    DateRangePicker(state = state, modifier = Modifier.height(420.dp), colors = colors, title = null, headline = null, showModeToggle = false)
    val start = state.selectedStartDateMillis
    val end = state.selectedEndDateMillis ?: start
    Spacer(Modifier.height(8.dp))
    SolidButton("导出这段时间", Modifier.fillMaxWidth(), enabled = start != null) {
        if (start != null && end != null) onPicked(utcDate(start), utcDate(end))
    }
}

/** The picker reports UTC midnight of the chosen day. */
private fun utcDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
