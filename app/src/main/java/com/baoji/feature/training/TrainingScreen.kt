package com.baoji.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
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
import com.baoji.core.icon.LineIcons
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.BaojiCard
import com.baoji.core.ui.BodyMap
import com.baoji.core.ui.BubbleGap
import com.baoji.core.ui.CatalogPickerSheet
import com.baoji.core.ui.CatalogPickerSpec
import com.baoji.core.ui.NoteBubble
import com.baoji.core.ui.NumberSheet
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.QuietButton
import com.baoji.core.ui.SwipeToDelete
import com.baoji.core.ui.UndoBar
import com.baoji.core.ui.Watermark
import com.baoji.core.ui.monthDay
import com.baoji.core.ui.pressable
import com.baoji.core.ui.trimmed
import com.baoji.data.catalog.ExerciseItem
import com.baoji.data.db.FreeLogEntity
import com.baoji.data.repo.ActivityDay
import com.baoji.data.repo.ActivityRepository
import com.baoji.data.repo.TrainingDay
import com.baoji.domain.CalorieEngine
import com.baoji.domain.ExerciseLoad
import com.baoji.domain.MuscleMap

@Composable
fun TrainingScreen(vm: TrainingViewModel = hiltViewModel()) {
    val day by vm.day.collectAsStateWithLifecycle()
    val extras by vm.extras.collectAsStateWithLifecycle()
    val activity by vm.activityDay.collectAsStateWithLifecycle()
    val weightKg by vm.weightKg.collectAsStateWithLifecycle()
    val editingId by vm.editingId.collectAsStateWithLifecycle()
    val undo by vm.undo.collectAsStateWithLifecycle()
    var showExtra by rememberSaveable { mutableStateOf(false) }
    var showSteps by rememberSaveable { mutableStateOf(false) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    val d = day ?: return

    Box(Modifier.fillMaxSize()) {
        TrainingContent(
            day = d,
            activity = activity,
            extras = extras,
            actions = TrainingActions(
                onSet = vm::completeSet,
                onReset = vm::reset,
                onArchive = vm::archive,
                onOpen = vm::openEditor,
                onSteps = { showSteps = true },
                onResetSteps = vm::resetSteps,
                onDeleteActivity = vm::deleteActivity,
                onExtra = { showExtra = true },
                onCalendar = { showCalendar = true },
            ),
        )
        UndoBar(undo, onUndo = vm::undo, onTimeout = { undo?.let(vm::dismissUndo) })
    }

    val editing = d.rows.firstOrNull { it.exercise.id == editingId }?.exercise
    if (editing != null) {
        val historyFlow = remember(editing.name) { vm.history(editing.name) }
        val history by historyFlow.collectAsState(initial = emptyList())
        ExerciseEditorSheet(
            exercise = editing,
            history = history,
            onAdjust = { w, r -> vm.adjust(editing.id, w, r) },
            onSets = { vm.setSets(editing.id, it) },
            onDismiss = vm::closeEditor,
        )
    }
    if (showCalendar) {
        CalendarSheet(onDismiss = { showCalendar = false })
    }
    if (showSteps) {
        NumberSheet(
            title = "今天的步数",
            unit = ActivityRepository.STEPS_UNIT,
            initial = activity.steps?.amount?.toInt(),
            max = ActivityRepository.MAX_STEPS,
            onDismiss = { showSteps = false },
            onSave = vm::setSteps,
        )
    }
    if (showExtra) {
        CatalogPickerSheet(
            spec = remember(weightKg) { activityPickerSpec(weightKg) },
            search = vm::searchExercises,
            onDismiss = { showExtra = false },
            onSave = vm::addTimed,
            onSaveText = vm::addExtra,
        )
    }
}

class TrainingActions(
    val onSet: (String) -> Unit = {},
    val onReset: (String) -> Unit = {},
    val onArchive: (String) -> Unit = {},
    val onOpen: (String) -> Unit = {},
    val onSteps: () -> Unit = {},
    val onResetSteps: () -> Unit = {},
    val onDeleteActivity: (String) -> Unit = {},
    val onExtra: () -> Unit = {},
    val onCalendar: () -> Unit = {},
)

private fun activityPickerSpec(weightKg: Double?) = CatalogPickerSpec<ExerciseItem>(
    title = "加练",
    searchHint = "搜索运动，如 跑步、游泳、羽毛球",
    emptyHint = "",
    itemTitle = { it.name },
    itemSubtitle = { "${it.category} · ${it.met.trimmed()} MET" },
    amountUnit = ActivityRepository.MINUTES,
    defaultAmount = "30",
    maxAmount = ActivityRepository.MAX_MINUTES.toDouble(),
    preview = { item, minutes ->
        weightKg?.let { "约 ${CalorieEngine.activityKcal(item.met, it, minutes).toLong()} kcal（已扣除静息消耗）" }
            ?: "先在体重页记一次体重，才能算消耗"
    },
)

@Composable
fun TrainingContent(day: TrainingDay, activity: ActivityDay, extras: List<FreeLogEntity>, actions: TrainingActions) {
    Box(Modifier.fillMaxSize()) {
        if (!day.isTrainingDay) Watermark("休息日")
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = PagePadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(BubbleGap),
        ) {
            item { DateHeader(day, actions.onCalendar) }
            if (day.isTrainingDay && day.rows.isNotEmpty()) {
                item(key = "body") {
                    val levels = remember(day.rows) { MuscleMap.levels(day.rows.map { ExerciseLoad(it.exercise.name, it.exercise.sets, it.done) }) }
                    BaojiCard { BodyMap(levels) }
                }
            }
            if (day.isTrainingDay) {
                items(day.rows, key = { it.exercise.id }) { row ->
                    val id = row.exercise.id
                    SwipeToDelete(label = row.exercise.name, onDelete = { actions.onArchive(id) }) {
                        ExerciseRowItem(
                            row = row,
                            onOpen = { actions.onOpen(id) },
                            onSet = { actions.onSet(id) },
                            onReset = { actions.onReset(id) },
                        )
                    }
                }
            }
            item(key = "steps") {
                StepsRowItem(activity.steps, onTap = actions.onSteps, onReset = actions.onResetSteps)
            }
            items(activity.timed, key = { it.id }) { log ->
                SwipeToDelete(label = log.name, onDelete = { actions.onDeleteActivity(log.id) }) { ActivityLogRow(log) }
            }
            items(extras, key = { it.id }) { e -> NoteBubble(e.text) }
            item {
                QuietButton("＋ 加练", onClick = actions.onExtra)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DateHeader(day: TrainingDay, onCalendar: () -> Unit) {
    val c = Baoji.colors
    Box(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Row(
            Modifier.pressable(label = "${day.date.monthDay()}，打开日历", onClick = onCalendar).semantics { heading() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(day.date.monthDay(), style = MaterialTheme.typography.headlineMedium, color = c.primaryText)
            Spacer(Modifier.width(6.dp))
            Icon(LineIcons.Down, contentDescription = null, tint = c.secondaryText, modifier = Modifier.size(22.dp))
        }
    }
}
