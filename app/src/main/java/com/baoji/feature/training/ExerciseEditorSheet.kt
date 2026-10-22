package com.baoji.feature.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baoji.core.icon.LineIcons
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.pressable
import com.baoji.core.ui.trimmed
import com.baoji.data.db.ExerciseEntity
import com.baoji.data.repo.DayMax
import com.baoji.data.repo.TrainingRepository

const val WEIGHT_STEP = 2.5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditorSheet(
    exercise: ExerciseEntity,
    history: List<DayMax>,
    onAdjust: (weight: Double, reps: Int) -> Unit,
    onSets: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Baoji.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.background,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding).navigationBarsPadding()) {
            Text(exercise.name, style = MaterialTheme.typography.titleLarge, color = c.primaryText)
            Text(
                "次数区间 ${exercise.repsMin}–${exercise.repsMax}",
                style = MaterialTheme.typography.bodySmall,
                color = c.secondaryText,
            )
            Spacer(Modifier.height(20.dp))
            Stepper(
                label = "重量",
                value = "${exercise.currentWeight.trimmed()} kg",
                onMinus = { onAdjust((exercise.currentWeight - WEIGHT_STEP).coerceAtLeast(0.0), exercise.currentReps) },
                onPlus = { onAdjust(exercise.currentWeight + WEIGHT_STEP, exercise.currentReps) },
            )
            Spacer(Modifier.height(12.dp))
            Stepper(
                label = "次数",
                value = "${exercise.currentReps}",
                onMinus = { onAdjust(exercise.currentWeight, (exercise.currentReps - 1).coerceAtLeast(1)) },
                onPlus = { onAdjust(exercise.currentWeight, exercise.currentReps + 1) },
            )
            Spacer(Modifier.height(12.dp))
            Stepper(
                label = "组数",
                value = "${exercise.sets}",
                onMinus = { onSets((exercise.sets - 1).coerceAtLeast(1)) },
                onPlus = { onSets((exercise.sets + 1).coerceAtMost(TrainingRepository.MAX_SETS)) },
            )
            Text("重量和次数只影响之后的组", style = MaterialTheme.typography.labelSmall, color = c.secondaryText,
                modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(20.dp))
            if (history.size >= 2) {
                MiniLine(history, Modifier.fillMaxWidth().height(64.dp))
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${history.first().weight.trimmed()}", style = MaterialTheme.typography.labelSmall, color = c.secondaryText)
                    Text("${history.last().weight.trimmed()}", style = MaterialTheme.typography.labelSmall, color = c.secondaryText)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val c = Baoji.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = c.secondaryText, modifier = Modifier.width(56.dp))
        RoundIcon(LineIcons.Minus, "减少$label", onMinus)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = c.primaryText,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        RoundIcon(LineIcons.Plus, "增加$label", onPlus)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = Baoji.colors
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(c.card).pressable(label = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.primaryText, modifier = Modifier.size(22.dp))
    }
}

/** Heaviest set per recent session. Plain polyline, no axes. */
@Composable
private fun MiniLine(points: List<DayMax>, modifier: Modifier) {
    val c = Baoji.colors
    val desc = points.joinToString("，") { "${it.date.monthValue}/${it.date.dayOfMonth} ${it.weight.trimmed()}kg" }
    Canvas(modifier.semantics { contentDescription = "最近最大重量：$desc" }) {
        val min = points.minOf { it.weight }
        val max = points.maxOf { it.weight }
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (points.size - 1)
        val pad = 6.dp.toPx()
        fun y(w: Double) = (pad + (1 - (w - min) / span) * (size.height - 2 * pad)).toFloat()
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = i * stepX
            if (i == 0) path.moveTo(x, y(p.weight)) else path.lineTo(x, y(p.weight))
        }
        drawPath(path, c.primaryText, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        points.forEachIndexed { i, p -> drawCircle(c.primaryText, 2.dp.toPx(), Offset(i * stepX, y(p.weight))) }
    }
}
