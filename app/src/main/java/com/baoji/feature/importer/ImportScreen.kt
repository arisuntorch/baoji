package com.baoji.feature.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.OutlineChoice
import com.baoji.core.ui.PageHeader
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.SolidButton
import com.baoji.core.ui.baojiFieldColors
import com.baoji.core.ui.pressable
import com.baoji.core.ui.trimmed
import com.baoji.data.io.ImportKind
import com.baoji.data.io.ImportMode

@Composable
fun ImportScreen(kind: ImportKind, onDone: () -> Unit, vm: ImportViewModel = hiltViewModel()) {
    val c = Baoji.colors
    val text by vm.text.collectAsStateWithLifecycle()
    val check by vm.check.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::loadFile) }
    LaunchedEffect(kind) { vm.setKind(kind) }
    LaunchedEffect(done) { if (done) onDone() }

    Column(Modifier.fillMaxSize().imePadding()) {
        PageHeader(if (kind == ImportKind.DIET) "导入饮食计划" else "导入训练计划", onBack = onDone)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = PagePadding),
        ) {
            Text(
                "先和你自己的 AI 讨论好计划，再把 JSON 粘贴到这里。",
                style = MaterialTheme.typography.bodySmall,
                color = c.secondaryText,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = vm::setText,
                placeholder = { Text("粘贴 JSON", color = c.secondaryText) },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 320.dp),
                colors = baojiFieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "从文件选择",
                style = MaterialTheme.typography.bodyMedium,
                color = c.primaryText,
                modifier = Modifier
                    .pressable(label = "从文件选择") { picker.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    .padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            when (val ck = check) {
                Check.Empty -> Unit
                is Check.Problems -> ck.issues.forEach {
                    Text("第 ${it.line} 行  ${it.message}", style = MaterialTheme.typography.bodySmall, color = c.primaryText,
                        modifier = Modifier.padding(vertical = 2.dp))
                }
                is Check.Training -> TrainingPreview(ck)
                is Check.Diet -> DietPreview(ck)
            }
            status?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.primaryText)
            }
            Spacer(Modifier.height(16.dp))
        }
        val valid = check is Check.Training || check is Check.Diet
        if (valid) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = PagePadding, vertical = 12.dp).navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlineChoice("合并", selected = false, modifier = Modifier.weight(1f)) { vm.commit(ImportMode.MERGE) }
                SolidButton("替换", Modifier.weight(1f)) { vm.commit(ImportMode.REPLACE) }
            }
            Text(
                "合并：同名更新，新的新增。替换：旧计划归档，历史记录保留。",
                style = MaterialTheme.typography.labelSmall,
                color = c.secondaryText,
                modifier = Modifier.padding(horizontal = PagePadding).padding(bottom = 12.dp).navigationBarsPadding(),
            )
        }
    }
}

private val WEEKDAYS = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
private fun TrainingPreview(ck: Check.Training) {
    val c = Baoji.colors
    ck.data.daily.forEach {
        Text("日常活动 · ${it.name} ${it.kcal.trimmed()} kcal/天", style = MaterialTheme.typography.bodySmall, color = c.secondaryText)
    }
    ck.data.plans.forEach { p ->
        Spacer(Modifier.height(10.dp))
        Text("${p.name} · 周${p.days.joinToString("、") { WEEKDAYS[it - 1] }}", style = MaterialTheme.typography.titleMedium, color = c.primaryText)
        p.exercises.forEach { e ->
            val reps = if (e.repsMin == e.repsMax) "${e.repsMin}" else "${e.repsMin}–${e.repsMax}"
            Text("${e.name}  ${e.weight.trimmed()}kg · ${e.sets}×$reps · ${e.kcalPerSet.trimmed()} kcal/组",
                style = MaterialTheme.typography.bodySmall, color = c.secondaryText)
        }
    }
}

@Composable
private fun DietPreview(ck: Check.Diet) {
    val c = Baoji.colors
    ck.data.targets?.let {
        Text("目标 蛋白 ${it.protein.trimmed()} · 碳水 ${it.carbs.trimmed()} · 脂肪 ${it.fat.trimmed()} g",
            style = MaterialTheme.typography.bodyMedium, color = c.primaryText)
        Spacer(Modifier.height(8.dp))
    }
    ck.data.meals.forEach { m ->
        Text("${m.name} ${m.amount.trimmed()}${m.unit} · ${m.kcal.toLong()} kcal",
            style = MaterialTheme.typography.bodySmall, color = c.secondaryText)
    }
}
