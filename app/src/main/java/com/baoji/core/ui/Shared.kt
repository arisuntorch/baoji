package com.baoji.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baoji.core.theme.Baoji
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val MONTH_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

fun LocalDate.monthDay(): String = format(MONTH_DAY)

/** Huge, barely visible background word such as 训练日 / 休息日. Hidden from screen readers. */
@Composable
fun Watermark(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Text(
            text,
            color = Baoji.colors.watermark,
            fontSize = 88.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
fun baojiFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Baoji.colors.primaryText,
    unfocusedBorderColor = Baoji.colors.separator,
    cursorColor = Baoji.colors.primaryText,
    focusedTextColor = Baoji.colors.primaryText,
    unfocusedTextColor = Baoji.colors.primaryText,
    focusedContainerColor = Baoji.colors.card,
    unfocusedContainerColor = Baoji.colors.card,
)

/** Natural-language note sheet used for 加练 and 加餐. Stores text only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeTextSheet(title: String, placeholder: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Baoji.colors.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding).imePadding().navigationBarsPadding()) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Baoji.colors.primaryText)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(500) },
                placeholder = { Text(placeholder, color = Baoji.colors.secondaryText) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).focusRequester(focus),
                colors = baojiFieldColors(),
            )
            Spacer(Modifier.height(16.dp))
            SolidButton("记下", Modifier.fillMaxWidth(), enabled = text.isNotBlank()) {
                onSave(text)
                onDismiss()
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/** Formats 60.0 → "60", 62.5 → "62.5". */
fun Double.trimmed(): String =
    if (this % 1.0 == 0.0) toLong().toString() else String.format(java.util.Locale.ROOT, "%.1f", this).trimEnd('0').trimEnd('.')
