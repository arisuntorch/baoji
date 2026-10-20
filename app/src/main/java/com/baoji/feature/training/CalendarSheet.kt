package com.baoji.feature.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.icon.LineIcons
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.BaojiCard
import com.baoji.core.ui.BareIconButton
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.monthDay
import com.baoji.core.ui.pressable
import com.baoji.core.ui.trimmed
import com.baoji.data.db.ExerciseEntity
import com.baoji.data.db.SetLogEntity
import com.baoji.data.repo.DayMark
import com.baoji.data.repo.DayPreview
import com.baoji.data.repo.DayTense
import java.time.LocalDate
import java.time.YearMonth

private val WEEKDAYS = listOf("一", "二", "三", "四", "五", "六", "日")
private val CellSize = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSheet(onDismiss: () -> Unit, vm: CalendarViewModel = hiltViewModel()) {
    val month by vm.month.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val marks by vm.marks.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.reset() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Baoji.colors.background,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding).navigationBarsPadding()) {
            MonthHeader(month, onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) })
            Spacer(Modifier.height(8.dp))
            MonthGrid(month, vm.today(), selected, marks, onSelect = vm::select)
            Spacer(Modifier.height(16.dp))
            preview?.takeIf { it.date == selected }?.let { DayPreviewCard(it) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${month.year}年${month.monthValue}月",
            style = MaterialTheme.typography.titleLarge,
            color = Baoji.colors.primaryText,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        BareIconButton(LineIcons.Back, "上个月", onClick = onPrev)
        BareIconButton(LineIcons.Forward, "下个月", onClick = onNext)
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    marks: Map<LocalDate, DayMark>,
    onSelect: (LocalDate) -> Unit,
) {
    val c = Baoji.colors
    Row(Modifier.fillMaxWidth()) {
        WEEKDAYS.forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = c.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    val lead = month.atDay(1).dayOfWeek.value - 1
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            (0 until 7).forEach { i ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    week.getOrNull(i)?.let { date ->
                        DayCell(date, date == today, date == selected, marks[date]) { onSelect(date) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isToday: Boolean, isSelected: Boolean, mark: DayMark?, onClick: () -> Unit) {
    val c = Baoji.colors
    val markText = when (mark) {
        DayMark.LOGGED -> "，有记录"
        DayMark.PLANNED -> "，有计划"
        null -> ""
    }
    Column(
        Modifier.padding(vertical = 2.dp).pressable(label = "${date.monthDay()}$markText", onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(CellSize)
                .then(if (isSelected) Modifier.background(c.primaryText, CircleShape) else Modifier)
                .then(if (isToday && !isSelected) Modifier.border(1.dp, c.primaryText, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) c.background else c.primaryText,
            )
        }
        Spacer(Modifier.height(3.dp))
        val dot = Modifier.size(6.dp)
        when (mark) {
            DayMark.LOGGED -> Box(dot.background(c.primaryText, CircleShape))
            DayMark.PLANNED -> Box(dot.border(1.dp, c.secondaryText, CircleShape))
            null -> Spacer(dot)
        }
    }
}

@Composable
private fun DayPreviewCard(p: DayPreview) {
    val c = Baoji.colors
    val showLogged = p.logged.isNotEmpty() && (p.tense == DayTense.PAST || p.planned.isEmpty())
    val title = buildString {
        append(p.date.monthDay()).append(" 周").append(WEEKDAYS[p.date.dayOfWeek.value - 1])
        if (p.planNames.isNotEmpty()) append(" · ").append(p.planNames.joinToString("、"))
    }
    BaojiCard {
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.primaryText)
        Spacer(Modifier.height(12.dp))
        when {
            showLogged -> p.logged.forEach { PreviewLine(it.name, setSummary(it.sets)) }
            p.planned.isNotEmpty() && p.tense == DayTense.PAST -> Quiet("这天没有记录")
            p.planned.isNotEmpty() -> p.planned.forEach { PreviewLine(it.name, planSummary(it)) }
            else -> Quiet("休息日")
        }
    }
}

@Composable
private fun PreviewLine(name: String, detail: String) {
    val c = Baoji.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.bodyLarge, color = c.primaryText, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = c.secondaryText, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
    }
}

@Composable
private fun Quiet(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = Baoji.colors.secondaryText, modifier = Modifier.padding(vertical = 6.dp))
}

private fun weightText(w: Double) = if (w > 0) "${w.trimmed()}kg" else "自重"

private fun planSummary(e: ExerciseEntity): String {
    val reps = if (e.repsMin == e.repsMax) "${e.repsMin}" else "${e.repsMin}–${e.repsMax}"
    return "${e.sets} 组 × $reps 次 · ${weightText(e.currentWeight)}"
}

/** "80kg × 5 · 3 组" when every set matched, otherwise each set in order. */
internal fun setSummary(sets: List<SetLogEntity>): String {
    val first = sets.first()
    return if (sets.all { it.weight == first.weight && it.reps == first.reps }) {
        "${weightText(first.weight)} × ${first.reps} · ${sets.size} 组"
    } else {
        sets.joinToString("  ") { "${weightText(it.weight)}×${it.reps}" }
    }
}
