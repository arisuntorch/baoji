package com.baoji.feature.training

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.FreeTextSheet
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.QuietButton
import com.baoji.core.ui.Watermark
import com.baoji.core.ui.monthDay
import com.baoji.core.ui.rememberReduceMotion
import com.baoji.data.db.FreeLogEntity
import com.baoji.data.repo.TrainingDay

@Composable
fun TrainingScreen(vm: TrainingViewModel = hiltViewModel()) {
    val day by vm.day.collectAsStateWithLifecycle()
    val extras by vm.extras.collectAsStateWithLifecycle()
    val editingId by vm.editingId.collectAsStateWithLifecycle()
    var showExtra by rememberSaveable { mutableStateOf(false) }
    val d = day ?: return

    TrainingContent(
        day = d,
        extras = extras,
        onCheckIn = vm::checkIn,
        onSet = vm::completeSet,
        onUndo = vm::undoSet,
        onOpen = vm::openEditor,
        onExtra = { showExtra = true },
    )

    val editing = d.rows.firstOrNull { it.exercise.id == editingId }?.exercise
    if (editing != null) {
        val historyFlow = remember(editing.name) { vm.history(editing.name) }
        val history by historyFlow.collectAsState(initial = emptyList())
        ExerciseEditorSheet(
            exercise = editing,
            history = history,
            onAdjust = { w, r -> vm.adjust(editing.id, w, r) },
            onDismiss = vm::closeEditor,
        )
    }
    if (showExtra) {
        FreeTextSheet("加练", "例如：跑步 30 分钟、今天走了 8000 步", onDismiss = { showExtra = false }, onSave = vm::addExtra)
    }
}

@Composable
fun TrainingContent(
    day: TrainingDay,
    extras: List<FreeLogEntity>,
    onCheckIn: () -> Unit,
    onSet: (String) -> Unit,
    onUndo: (String) -> Unit,
    onOpen: (String) -> Unit,
    onExtra: () -> Unit,
) {
    val c = Baoji.colors
    val reduce = rememberReduceMotion()
    Box(Modifier.fillMaxSize()) {
        if (!day.isTrainingDay) Watermark("休息日")
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = PagePadding, vertical = 8.dp)) {
            item {
                Text(
                    day.date.monthDay(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = c.primaryText,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).semantics { heading() },
                )
            }
            if (day.isTrainingDay) {
                item {
                    AnimatedVisibility(
                        visible = day.showCheckIn,
                        enter = fadeIn(tween(if (reduce) 0 else 300)),
                        exit = fadeOut(tween(if (reduce) 0 else 600)) + shrinkVertically(tween(if (reduce) 0 else 600)),
                    ) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            CheckInButton(onClick = onCheckIn)
                        }
                    }
                }
                items(day.rows, key = { it.exercise.id }) { row ->
                    Column {
                        ExerciseRowItem(
                            row = row,
                            onOpen = { onOpen(row.exercise.id) },
                            onSet = { onSet(row.exercise.id) },
                            onUndo = { onUndo(row.exercise.id) },
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = c.separator)
                    }
                }
            }
            items(extras, key = { it.id }) { e ->
                Text(
                    e.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.secondaryText,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                QuietButton("＋ 加练", onClick = onExtra)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
