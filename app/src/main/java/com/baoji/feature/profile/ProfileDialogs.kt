package com.baoji.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.baojiFieldColors
import com.baoji.core.ui.trimmed
import com.baoji.data.prefs.NutritionTargets

@Composable
internal fun NumberDialog(
    title: String,
    initial: String,
    type: KeyboardType,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    BaojiAlert(title = title, confirm = "保存", onConfirm = { onSave(value); onDismiss() }, onDismiss = onDismiss) {
        Field(value, { value = it }, null, type)
    }
}

@Composable
internal fun TargetsDialog(
    targets: NutritionTargets,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var p by rememberSaveable { mutableStateOf(targets.protein.trimmed()) }
    var c by rememberSaveable { mutableStateOf(targets.carbs.trimmed()) }
    var f by rememberSaveable { mutableStateOf(targets.fat.trimmed()) }
    BaojiAlert(title = "营养目标（克）", confirm = "保存", onConfirm = { onSave(p, c, f); onDismiss() }, onDismiss = onDismiss) {
        Column {
            Field(p, { p = it }, "蛋白", KeyboardType.Decimal)
            Field(c, { c = it }, "碳水", KeyboardType.Decimal)
            Field(f, { f = it }, "脂肪", KeyboardType.Decimal)
        }
    }
}

@Composable
internal fun DataDialogs(dialog: DataDialog?, vm: ProfileViewModel) {
    when (dialog) {
        is DataDialog.ConfirmRestore -> BaojiAlert(
            title = "从备份恢复",
            confirm = "覆盖并恢复",
            onConfirm = { vm.confirmRestore(dialog.backup) },
            onDismiss = vm::dismissDialog,
        ) {
            Text(
                "备份包含 ${dialog.backup.days.size} 天的记录。恢复会覆盖当前全部数据。",
                style = MaterialTheme.typography.bodyMedium,
                color = Baoji.colors.primaryText,
            )
        }
        DataDialog.ConfirmClear -> BaojiAlert(
            title = "清空数据",
            confirm = "继续",
            onConfirm = vm::confirmClearOnce,
            onDismiss = vm::dismissDialog,
        ) {
            Text("将删除所有记录、计划和设置。建议先导出一份全部数据。",
                style = MaterialTheme.typography.bodyMedium, color = Baoji.colors.primaryText)
        }
        DataDialog.ConfirmClearAgain -> BaojiAlert(
            title = "再次确认",
            confirm = "全部清空",
            onConfirm = vm::confirmClearFinal,
            onDismiss = vm::dismissDialog,
        ) {
            Text("清空后无法撤销。", style = MaterialTheme.typography.bodyMedium, color = Baoji.colors.primaryText)
        }
        null -> Unit
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String?, type: KeyboardType) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' }.take(6)) },
        label = label?.let { { Text(it, color = Baoji.colors.secondaryText) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        colors = baojiFieldColors(),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
private fun BaojiAlert(
    title: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = Baoji.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.card,
        title = { Text(title, color = c.primaryText) },
        text = content,
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = c.primaryText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = c.secondaryText) } },
    )
}
