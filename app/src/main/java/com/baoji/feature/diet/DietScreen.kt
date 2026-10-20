package com.baoji.feature.diet

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.icon.LineIcons
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.FreeTextSheet
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.QuietButton
import com.baoji.core.ui.motion
import com.baoji.core.ui.pressable
import com.baoji.core.ui.rememberReduceMotion
import com.baoji.core.ui.softSpring
import com.baoji.core.ui.trimmed
import com.baoji.data.db.FreeLogEntity
import com.baoji.data.repo.DietDay
import com.baoji.data.repo.MealRow

@Composable
fun DietScreen(vm: DietViewModel = hiltViewModel()) {
    val day by vm.day.collectAsStateWithLifecycle()
    val snacks by vm.snacks.collectAsStateWithLifecycle()
    var showSnack by rememberSaveable { mutableStateOf(false) }
    val d = day ?: return
    DietContent(d, snacks, onToggle = vm::toggle, onSnack = { showSnack = true })
    if (showSnack) {
        FreeTextSheet("加餐", "例如：711 茶叶蛋一个", onDismiss = { showSnack = false }, onSave = vm::addSnack)
    }
}

@Composable
fun DietContent(day: DietDay, snacks: List<FreeLogEntity>, onToggle: (String) -> Unit, onSnack: () -> Unit) {
    val c = Baoji.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = PagePadding, vertical = 8.dp)) {
        item {
            MacroBar("蛋白", day.eaten.protein, day.targets.protein)
            MacroBar("碳水", day.eaten.carbs, day.targets.carbs)
            MacroBar("脂肪", day.eaten.fat, day.targets.fat)
            Text(
                "${day.eaten.kcal.toLong()} kcal",
                style = MaterialTheme.typography.bodySmall,
                color = c.secondaryText,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
        }
        items(day.rows, key = { it.task.id }) { row ->
            Column {
                MealRowItem(row, onToggle = { onToggle(row.task.id) })
                HorizontalDivider(thickness = 0.5.dp, color = c.separator)
            }
        }
        items(snacks, key = { it.id }) { s ->
            Text(s.text, style = MaterialTheme.typography.bodyMedium, color = c.secondaryText,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        }
        item {
            Spacer(Modifier.height(8.dp))
            QuietButton("＋ 加餐", onClick = onSnack)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Horizontal bar; over target simply stays full — no colour change, no warning. */
@Composable
private fun MacroBar(name: String, eaten: Double, target: Double) {
    val c = Baoji.colors
    val reduce = rememberReduceMotion()
    val fraction = if (target <= 0) 0f else (eaten / target).toFloat().coerceIn(0f, 1f)
    val shown by animateFloatAsState(fraction, motion(reduce, softSpring()), label = "bar$name")
    val targetText = if (target > 0) target.trimmed() else "—"
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics(mergeDescendants = true) {
            contentDescription = "$name 已摄入 ${eaten.trimmed()} 克，目标 $targetText 克"
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, color = c.primaryText, modifier = Modifier.width(44.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.fill)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(shown).clip(RoundedCornerShape(4.dp)).background(c.primaryText))
        }
        Text(
            "${eaten.trimmed()} / $targetText g",
            style = MaterialTheme.typography.bodySmall,
            color = c.secondaryText,
            modifier = Modifier.padding(start = 10.dp).width(92.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun MealRowItem(row: MealRow, onToggle: () -> Unit) {
    val c = Baoji.colors
    val t = row.task
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                stateDescription = if (row.checked) "已吃" else "未吃"
            }
            .pressable(label = t.name, onClick = onToggle)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LineIcons.forFood(t.name), contentDescription = null, tint = c.primaryText, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Text(t.name, style = MaterialTheme.typography.titleMedium, color = c.primaryText, modifier = Modifier.weight(1f))
        Text("${t.amount.trimmed()} ${t.unit}", style = MaterialTheme.typography.bodyMedium, color = c.secondaryText)
        Spacer(Modifier.width(16.dp))
        CheckCircle(row.checked)
    }
}

/** Circle that draws its tick in a single stroke when checked. */
@Composable
private fun CheckCircle(checked: Boolean) {
    val c = Baoji.colors
    val reduce = rememberReduceMotion()
    val progress by animateFloatAsState(if (checked) 1f else 0f, motion(reduce, tween(320)), label = "tick")
    Canvas(Modifier.size(28.dp)) {
        val r = size.minDimension / 2
        if (progress > 0f) {
            drawCircle(c.primaryText, r)
        } else {
            drawCircle(c.secondaryText, r - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
        }
        if (progress > 0f) {
            val tick = Path().apply {
                moveTo(size.width * 0.28f, size.height * 0.52f)
                lineTo(size.width * 0.44f, size.height * 0.67f)
                lineTo(size.width * 0.73f, size.height * 0.36f)
            }
            val measure = PathMeasure().apply { setPath(tick, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * progress, partial, true)
            drawPath(partial, c.background, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
