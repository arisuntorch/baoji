package com.baoji.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.OutlineChoice
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.QuietButton
import com.baoji.core.ui.SolidButton
import com.baoji.core.ui.baojiFieldColors
import com.baoji.data.io.ImportKind
import com.baoji.domain.Sex
import com.baoji.feature.importer.ImportScreen

@Composable
fun OnboardingScreen(onFinished: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    val form by vm.form.collectAsStateWithLifecycle()
    // Optional steps reuse the import screen in place; "跳过" or a successful import moves on.
    var importing by rememberSaveable { mutableStateOf(false) }
    when (form.step) {
        OnboardingStep.PROFILE -> ProfileStep(
            form = form,
            onHeight = vm::setHeight,
            onWeight = vm::setWeight,
            onAge = vm::setAge,
            onSex = vm::setSex,
            onSubmit = vm::submitProfile,
        )
        OnboardingStep.TRAINING -> if (importing) {
            ImportScreen(ImportKind.TRAINING, onDone = { importing = false; vm.next() }, vm = hiltViewModel(key = "ob-training"))
        } else {
            OptionalStep("导入训练计划", "也可以之后在训练页左上角导入。", onImport = { importing = true }, onSkip = vm::next)
        }
        OnboardingStep.DIET -> if (importing) {
            ImportScreen(ImportKind.DIET, onDone = { importing = false; onFinished() }, vm = hiltViewModel(key = "ob-diet"))
        } else {
            OptionalStep("导入饮食计划", "也可以之后在饮食页左上角导入。", onImport = { importing = true }, onSkip = onFinished)
        }
    }
}

@Composable
fun ProfileStep(
    form: OnboardingForm,
    onHeight: (String) -> Unit,
    onWeight: (String) -> Unit,
    onAge: (String) -> Unit,
    onSex: (Sex) -> Unit,
    onSubmit: () -> Unit,
) {
    val c = Baoji.colors
    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = PagePadding),
    ) {
        Spacer(Modifier.height(48.dp))
        Text("你好", style = MaterialTheme.typography.headlineMedium, color = c.primaryText)
        Text("先填四项基本信息，用来估算每天的消耗。", style = MaterialTheme.typography.bodyMedium, color = c.secondaryText)
        Spacer(Modifier.height(28.dp))
        NumberField("身高 cm", form.height, onHeight, KeyboardType.Decimal, "height")
        NumberField("体重 kg", form.weight, onWeight, KeyboardType.Decimal, "weight")
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineChoice("男", form.sex == Sex.MALE, Modifier.weight(1f)) { onSex(Sex.MALE) }
            OutlineChoice("女", form.sex == Sex.FEMALE, Modifier.weight(1f)) { onSex(Sex.FEMALE) }
        }
        NumberField("年龄", form.age, onAge, KeyboardType.Number, "age")
        form.errors.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = c.primaryText) }
        Spacer(Modifier.height(24.dp))
        SolidButton("继续", Modifier.fillMaxWidth().testTag("submit"), enabled = form.complete, onClick = onSubmit)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, type: KeyboardType, tag: String) {
    val c = Baoji.colors
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = c.secondaryText) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type, imeAction = ImeAction.Next),
        colors = baojiFieldColors(),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag(tag),
    )
}

@Composable
private fun OptionalStep(title: String, hint: String, onImport: () -> Unit, onSkip: () -> Unit) {
    val c = Baoji.colors
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = PagePadding)) {
        Column(Modifier.fillMaxWidth().padding(top = 48.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = c.primaryText)
            Text("可选。$hint", style = MaterialTheme.typography.bodyMedium, color = c.secondaryText)
        }
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp).align(androidx.compose.ui.Alignment.BottomCenter)) {
            SolidButton("导入", Modifier.fillMaxWidth(), onClick = onImport)
            Spacer(Modifier.height(8.dp))
            QuietButton("跳过", onClick = onSkip)
        }
    }
}
