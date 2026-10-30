package com.baoji.feature.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.baoji.core.icon.LineIcons
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.CardShape
import com.baoji.core.ui.IconTile
import com.baoji.core.ui.pressable
import com.baoji.core.ui.trimmed
import com.baoji.data.db.ActivityKind
import com.baoji.data.db.ActivityLogEntity
import com.baoji.data.repo.ActivityRepository
import com.baoji.domain.CalorieEngine
import java.text.NumberFormat
import java.util.Locale

/** The daily-steps plan item, shown every day. Tap to enter today's count, long press to clear it. */
@Composable
fun StepsRowItem(steps: ActivityLogEntity?, onTap: () -> Unit, onReset: () -> Unit) {
    val c = Baoji.colors
    val detail = steps?.let { "${formatSteps(it.amount)} 步 · ${it.kcal.toLong()} kcal" } ?: DEFAULT_STEPS_HINT
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.card)
            .semantics(mergeDescendants = true) { stateDescription = detail }
            .pressable(label = "填写${ActivityRepository.STEPS_NAME}", onLongClick = onReset, onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(LineIcons.forExercise("步行"))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(ActivityRepository.STEPS_NAME, style = MaterialTheme.typography.titleLarge, color = c.primaryText)
            Spacer(Modifier.height(4.dp))
            Text(detail, style = MaterialTheme.typography.bodyLarge, color = c.secondaryText)
        }
    }
}

/** A timed activity or strength move picked from the built-in list. */
@Composable
fun ActivityLogRow(log: ActivityLogEntity) {
    val c = Baoji.colors
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(c.card).padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(log.name, style = MaterialTheme.typography.bodyLarge, color = c.primaryText)
            Text(activityDetail(log), style = MaterialTheme.typography.bodyMedium, color = c.secondaryText)
        }
        Text("${log.kcal.toLong()} kcal", style = MaterialTheme.typography.bodyMedium, color = c.secondaryText)
    }
}

/** "3 组 × 12 次 · 6kg" for strength sets, "30 分钟" otherwise. */
fun activityDetail(log: ActivityLogEntity): String {
    if (log.kind != ActivityKind.SETS) return "${log.amount.trimmed()} ${log.unit}"
    val load = log.weightKg?.let { "${it.trimmed()}kg" } ?: "自重"
    return "${log.amount.toInt()} 组 × ${log.reps ?: 0} 次 · $load"
}

/** Shown while today has no count; [CalorieEngine.burnedAt] counts the default from 23:45. */
private val DEFAULT_STEPS_HINT =
    "未填写，${CalorieEngine.DEFAULT_STEPS_AT} 起按 ${formatSteps(CalorieEngine.DEFAULT_STEPS.toDouble())} 步计"

private fun formatSteps(amount: Double): String = NumberFormat.getIntegerInstance(Locale.ROOT).format(amount.toLong())
