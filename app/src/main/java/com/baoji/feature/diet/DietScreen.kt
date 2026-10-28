package com.baoji.feature.diet

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.icon.LineIcons
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.BaojiCard
import com.baoji.core.ui.BubbleGap
import com.baoji.core.ui.CatalogPickerSheet
import com.baoji.core.ui.CatalogPickerSpec
import com.baoji.core.ui.SwipeToDelete
import com.baoji.core.ui.UndoBar
import com.baoji.core.ui.CardShape
import com.baoji.core.ui.IconTile
import com.baoji.core.ui.NoteBubble
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.QuietButton
import com.baoji.core.ui.monthDay
import com.baoji.core.ui.motion
import com.baoji.core.ui.pressable
import com.baoji.core.ui.rememberReduceMotion
import com.baoji.core.ui.softSpring
import com.baoji.core.ui.trimmed
import com.baoji.data.catalog.FoodItem
import com.baoji.data.db.FreeLogEntity
import com.baoji.data.db.MealLogEntity
import com.baoji.data.repo.DietDay
import com.baoji.data.repo.MealRow

@Composable
fun DietScreen(vm: DietViewModel = hiltViewModel()) {
    val day by vm.day.collectAsStateWithLifecycle()
    val snacks by vm.snacks.collectAsStateWithLifecycle()
    val undo by vm.undo.collectAsStateWithLifecycle()
    var showSnack by rememberSaveable { mutableStateOf(false) }
    val d = day ?: return
    Box(Modifier.fillMaxSize()) {
        DietContent(
            d, snacks,
            actions = MealActions(
                onToggle = vm::toggle, onReset = vm::reset, onArchive = vm::archive, onDeleteExtra = vm::deleteExtra,
            ),
            onSnack = { showSnack = true },
        )
        UndoBar(undo, onUndo = vm::undo, onTimeout = { undo?.let(vm::dismissUndo) })
    }
    if (showSnack) {
        CatalogPickerSheet(
            spec = FoodPickerSpec,
            search = vm::searchFoods,
            onDismiss = { showSnack = false },
            onSave = vm::addFood,
            onSaveText = vm::addSnack,
        )
    }
}

class MealActions(
    val onToggle: (String) -> Unit,
    val onReset: (String) -> Unit = {},
    val onArchive: (String) -> Unit = {},
    val onDeleteExtra: (String) -> Unit = {},
)

private val FoodPickerSpec = CatalogPickerSpec<FoodItem>(
    title = "加餐",
    searchHint = "搜索食物，如 米饭、鸡蛋、牛奶",
    emptyHint = "数据来自《中国食物成分表》，按可食部分称重",
    itemTitle = { it.name },
    itemSubtitle = { "${it.category} · 每100克 ${it.kcal.trimmed()} kcal" },
    amountUnit = "克",
    defaultAmount = "100",
    maxAmount = 5000.0,
    preview = { food, g ->
        val m = food.macrosFor(g)
        "约 ${m.kcal.toLong()} kcal · 蛋白 ${m.protein.trimmed()} · 碳水 ${m.carbs.trimmed()} · 脂肪 ${m.fat.trimmed()} 克"
    },
)

@Composable
fun DietContent(day: DietDay, snacks: List<FreeLogEntity>, actions: MealActions, onSnack: () -> Unit) {
    val c = Baoji.colors
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = PagePadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(BubbleGap),
    ) {
        item {
            Text(
                day.date.monthDay(),
                style = MaterialTheme.typography.headlineMedium,
                color = c.primaryText,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).semantics { heading() },
            )
        }
        item {
            BaojiCard {
                MacroBar("蛋白", day.eaten.protein, day.targets.protein)
                MacroBar("碳水", day.eaten.carbs, day.targets.carbs)
                MacroBar("脂肪", day.eaten.fat, day.targets.fat)
                Text(
                    "${day.eaten.kcal.toLong()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.secondaryText,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        items(day.rows, key = { it.task.id }) { row ->
            SwipeToDelete(label = row.task.name, onDelete = { actions.onArchive(row.task.id) }) {
                MealRowItem(row, onToggle = { actions.onToggle(row.task.id) }, onReset = { actions.onReset(row.task.id) })
            }
        }
        items(day.extras, key = { it.id }) { log ->
            SwipeToDelete(label = log.name, onDelete = { actions.onDeleteExtra(log.id) }) { ExtraFoodRow(log) }
        }
        items(snacks, key = { it.id }) { s -> NoteBubble(s.text) }
        item {
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
        Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {
            contentDescription = "$name 已摄入 ${eaten.trimmed()} 克，目标 $targetText 克"
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge, color = c.primaryText, modifier = Modifier.width(52.dp))
        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(c.fill)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(shown).clip(RoundedCornerShape(5.dp)).background(c.primaryText))
        }
        Text(
            "${eaten.trimmed()} / $targetText g",
            style = MaterialTheme.typography.bodyMedium,
            color = c.secondaryText,
            modifier = Modifier.padding(start = 12.dp).width(104.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun MealRowItem(row: MealRow, onToggle: () -> Unit, onReset: () -> Unit) {
    val c = Baoji.colors
    val t = row.task
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.card)
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                stateDescription = if (row.checked) "已吃" else "未吃"
            }
            .pressable(label = t.name, onLongClick = onReset, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(LineIcons.forFood(t.name))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, style = MaterialTheme.typography.titleLarge, color = c.primaryText)
            Spacer(Modifier.height(4.dp))
            Text("${t.amount.trimmed()} ${t.unit}", style = MaterialTheme.typography.bodyLarge, color = c.secondaryText)
        }
        Spacer(Modifier.width(16.dp))
        CheckCircle(row.checked)
    }
}

/** A food logged from the built-in table: name, grams and its kcal snapshot. */
@Composable
private fun ExtraFoodRow(log: MealLogEntity) {
    val c = Baoji.colors
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(c.card).padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(log.name, style = MaterialTheme.typography.bodyLarge, color = c.primaryText)
            Text(
                "${log.amount.trimmed()} ${log.unit} · 蛋白 ${log.protein.trimmed()} 克",
                style = MaterialTheme.typography.bodyMedium,
                color = c.secondaryText,
            )
        }
        Text("${log.kcal.toLong()} kcal", style = MaterialTheme.typography.bodyMedium, color = c.secondaryText)
    }
}

/** Circle that draws its tick in a single stroke when checked. */
@Composable
private fun CheckCircle(checked: Boolean) {
    val c = Baoji.colors
    val reduce = rememberReduceMotion()
    val progress by animateFloatAsState(if (checked) 1f else 0f, motion(reduce, tween(320)), label = "tick")
    Canvas(Modifier.size(40.dp)) {
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
