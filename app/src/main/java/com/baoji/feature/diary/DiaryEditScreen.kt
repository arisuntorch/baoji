package com.baoji.feature.diary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.PageHeader
import com.baoji.core.ui.PagePadding
import java.time.LocalDate

/** Opens straight into writing. Saved automatically; there is no save button. */
@Composable
fun DiaryEditScreen(onBack: () -> Unit, vm: DiaryEditViewModel = hiltViewModel()) {
    val c = Baoji.colors
    val text by vm.text.collectAsStateWithLifecycle()
    val date = LocalDate.ofEpochDay(vm.epochDay)
    val focus = remember { FocusRequester() }
    Column(Modifier.fillMaxSize().imePadding()) {
        PageHeader("${date.monthValue}/${date.dayOfMonth}", onBack)
        val value = text ?: return@Column
        BasicTextField(
            value = value,
            onValueChange = vm::setText,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.primaryText),
            cursorBrush = SolidColor(c.primaryText),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PagePadding)
                .navigationBarsPadding()
                .focusRequester(focus)
                .semantics { contentDescription = "日记内容" },
            decorationBox = { inner ->
                if (value.isEmpty()) Text("写点什么", style = MaterialTheme.typography.bodyLarge, color = c.secondaryText)
                inner()
            },
        )
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    }
}
