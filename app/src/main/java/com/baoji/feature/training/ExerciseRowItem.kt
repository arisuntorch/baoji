package com.baoji.feature.training

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.baoji.core.icon.LineIcons
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.CardShape
import com.baoji.core.ui.IconTile
import com.baoji.core.ui.motion
import com.baoji.core.ui.pressable
import com.baoji.core.ui.rememberReduceMotion
import com.baoji.core.ui.trimmed
import com.baoji.data.repo.ExerciseRow

@Composable
fun ExerciseRowItem(
    row: ExerciseRow,
    onOpen: () -> Unit,
    onSet: () -> Unit,
    onUndo: () -> Unit,
) {
    val c = Baoji.colors
    val e = row.exercise
    val reduce = rememberReduceMotion()
    val rowAlpha by animateFloatAsState(if (row.complete) 0.4f else 1f, motion(reduce, tween(400)), label = "rowAlpha")
    val strike by animateFloatAsState(if (row.complete) 1f else 0f, motion(reduce, tween(450)), label = "strike")

    Row(
        Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .clip(CardShape)
            .background(c.card)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).pressable(label = "编辑${e.name}", onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(LineIcons.forExercise(e.name))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    e.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.primaryText,
                    modifier = Modifier.drawWithContent {
                        drawContent()
                        if (strike > 0f) {
                            // Overhang past both ends: CJK names have a stroke right at mid-height that would hide a text-width line.
                            val overhang = 6.dp.toPx()
                            val y = size.height / 2
                            val end = -overhang + (size.width + 2 * overhang) * strike
                            drawLine(c.primaryText, Offset(-overhang, y), Offset(end, y), strokeWidth = 1.dp.toPx())
                        }
                    },
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${e.currentWeight.trimmed()}kg × ${e.currentReps}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.secondaryText,
                    )
                    if (row.overload) {
                        // The only nudge in the app: same colour and size as secondary text, no number.
                        Text("↑", style = MaterialTheme.typography.bodyLarge, color = c.secondaryText,
                            modifier = Modifier.semantics { contentDescription = "可以加重" })
                    }
                }
                Spacer(Modifier.height(10.dp))
                SetDots(done = row.done, total = e.sets)
            }
        }
        SetButton(done = row.done, total = e.sets, complete = row.complete, name = e.name, onTap = onSet, onLongPress = onUndo)
    }
}

@Composable
private fun SetDots(done: Int, total: Int) {
    val c = Baoji.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val filled = i < done
            val color by animateColorAsState(if (filled) c.primaryText else c.secondaryText, label = "dot$i")
            Canvas(Modifier.size(9.dp)) {
                if (filled) {
                    drawCircle(color)
                } else {
                    drawCircle(color, radius = size.minDimension / 2 - 0.5.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                }
            }
        }
    }
}

@Composable
private fun SetButton(done: Int, total: Int, complete: Boolean, name: String, onTap: () -> Unit, onLongPress: () -> Unit) {
    val c = Baoji.colors
    val fill by animateColorAsState(if (complete) c.primaryText else c.background, label = "setFill")
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, if (complete) c.primaryText else c.secondaryText, CircleShape)
            .semantics { stateDescription = "$name 已完成 $done 组，共 $total 组" }
            .pressable(label = "完成一组", onLongClick = onLongPress, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$done/$total",
            style = MaterialTheme.typography.titleMedium,
            color = if (complete) c.background else c.primaryText,
        )
    }
}
