package com.baoji.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.AvatarImage
import com.baoji.core.ui.BaojiCard
import com.baoji.core.ui.OutlineChoice
import com.baoji.core.ui.PageHeader
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.pressable
import com.baoji.core.ui.trimmed
import com.baoji.data.prefs.NutritionTargets
import com.baoji.data.prefs.ThemeMode
import com.baoji.data.prefs.VibrationLevel
import com.baoji.domain.Sex

private enum class EditField { HEIGHT, WEIGHT, AGE, TARGETS }

@Composable
fun ProfileScreen(onBack: () -> Unit, onCleared: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val c = Baoji.colors
    val profile by vm.profile.collectAsStateWithLifecycle()
    val targets by vm.targets.collectAsStateWithLifecycle()
    val vibration by vm.vibration.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val dialog by vm.dialog.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val cleared by vm.cleared.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<EditField?>(null) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::setAvatar) }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::pickBackup) }
    LaunchedEffect(cleared) { if (cleared) onCleared() }

    Column(Modifier.fillMaxSize()) {
        PageHeader(null, onBack)
        val p = profile ?: return@Column
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PagePadding).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AvatarImage(
                    p.avatarPath,
                    iconSize = 40.dp,
                    modifier = Modifier.size(88.dp).clip(CircleShape).background(c.card)
                        .pressable(label = "更换头像") {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                )
            }
            BaojiCard {
                InfoRow("身高", "${p.heightCm.trimmed()} cm") { editing = EditField.HEIGHT }
                Divider()
                InfoRow("体重", p.latestWeightKg?.let { "${it.trimmed()} kg" } ?: "—") { editing = EditField.WEIGHT }
                Divider()
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("性别", style = MaterialTheme.typography.bodyLarge, color = c.primaryText, modifier = Modifier.weight(1f))
                    OutlineChoice("男", p.sex == Sex.MALE) { vm.setSex(Sex.MALE) }
                    Spacer(Modifier.size(8.dp))
                    OutlineChoice("女", p.sex == Sex.FEMALE) { vm.setSex(Sex.FEMALE) }
                }
                Divider()
                InfoRow("年龄", "${p.age}") { editing = EditField.AGE }
            }
            SectionTitle("营养目标")
            BaojiCard {
                InfoRow(
                    "蛋白 / 碳水 / 脂肪",
                    if (targets == NutritionTargets.EMPTY) "未设置"
                    else "${targets.protein.trimmed()} / ${targets.carbs.trimmed()} / ${targets.fat.trimmed()} g",
                ) { editing = EditField.TARGETS }
            }
            SectionTitle("外观")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ThemeMode.SYSTEM to "跟随系统", ThemeMode.LIGHT to "浅色", ThemeMode.DARK to "深色")
                    .forEach { (mode, label) ->
                        OutlineChoice(label, themeMode == mode, Modifier.weight(1f)) { vm.setThemeMode(mode) }
                    }
            }
            SectionTitle("震动")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(VibrationLevel.OFF to "关", VibrationLevel.LIGHT to "弱", VibrationLevel.STANDARD to "标准", VibrationLevel.STRONG to "强")
                    .forEach { (level, label) ->
                        OutlineChoice(label, vibration == level, Modifier.weight(1f)) { vm.setVibration(level) }
                    }
            }
            SectionTitle("数据管理")
            BaojiCard {
                InfoRow("从备份恢复", "") { backupPicker.launch(arrayOf("application/json", "text/plain", "*/*")) }
                Divider()
                InfoRow("清空数据", "") { vm.askClear() }
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.secondaryText,
                    modifier = Modifier.pressable(label = "关闭提示", onClick = vm::clearMessage))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    val p = profile
    when (editing) {
        EditField.HEIGHT -> NumberDialog("身高 cm", p?.heightCm?.trimmed().orEmpty(), KeyboardType.Decimal,
            onDismiss = { editing = null }) { vm.setHeight(it) }
        EditField.WEIGHT -> NumberDialog("今天体重 kg", p?.latestWeightKg?.trimmed().orEmpty(), KeyboardType.Decimal,
            onDismiss = { editing = null }) { vm.setWeight(it) }
        EditField.AGE -> NumberDialog("年龄", p?.age?.toString().orEmpty(), KeyboardType.Number,
            onDismiss = { editing = null }) { vm.setAge(it) }
        EditField.TARGETS -> TargetsDialog(targets, onDismiss = { editing = null }) { a, b, d -> vm.setTargets(a, b, d) }
        null -> Unit
    }
    DataDialogs(dialog, vm)
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Baoji.colors.secondaryText,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp))
}

@Composable
private fun Divider() = HorizontalDivider(thickness = 0.5.dp, color = Baoji.colors.separator)

@Composable
private fun InfoRow(label: String, value: String, onClick: () -> Unit) {
    val c = Baoji.colors
    Row(
        Modifier.fillMaxWidth().pressable(label = label, onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = c.primaryText, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = c.secondaryText)
    }
}
