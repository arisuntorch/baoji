package com.baoji.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.baoji.core.theme.Baoji
import kotlinx.coroutines.delay

/** How a picker describes and measures its items, e.g. foods in grams or activities in minutes. */
class CatalogPickerSpec<T>(
    val title: String,
    val searchHint: String,
    val emptyHint: String,
    val itemTitle: (T) -> String,
    val itemSubtitle: (T) -> String,
    val amountUnit: String,
    val defaultAmount: String,
    val maxAmount: Double,
    /** One line shown under the amount field, e.g. "约 290 kcal". */
    val preview: (T, Double) -> String,
    /** Items entered as sets × reps × weight instead of a single amount, e.g. 卧推. */
    val usesSets: (T) -> Boolean = { false },
    /** One line shown under the sets fields, given the number of sets. */
    val setsPreview: (T, Int) -> String = { _, _ -> "" },
)

/** What was entered for a strength move; a null [weightKg] means bodyweight. */
data class SetsEntry(val sets: Int, val reps: Int, val weightKg: Double?)

/**
 * Two-step sheet: search the built-in table, then enter an amount. When nothing fits,
 * the typed text can still be kept as a plain note through [onSaveText].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CatalogPickerSheet(
    spec: CatalogPickerSpec<T>,
    search: suspend (String) -> List<T>,
    onDismiss: () -> Unit,
    onSave: (T, Double) -> Unit,
    onSaveText: (String) -> Unit,
    onSaveSets: (T, SetsEntry) -> Unit = { _, _ -> },
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<T?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Baoji.colors.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding).imePadding().navigationBarsPadding()) {
            Text(spec.title, style = MaterialTheme.typography.titleMedium, color = Baoji.colors.primaryText)
            Spacer(Modifier.height(12.dp))
            val pick = selected
            if (pick == null) {
                SearchStep(spec, query, onQuery = { query = it.take(40) }, search, onPick = { selected = it }, onSaveText = {
                    onSaveText(query.trim())
                    onDismiss()
                })
            } else if (spec.usesSets(pick)) {
                SetsStep(spec, pick, onBack = { selected = null }, onSave = { entry ->
                    onSaveSets(pick, entry)
                    onDismiss()
                })
            } else {
                AmountStep(spec, pick, onBack = { selected = null }, onSave = { amount ->
                    onSave(pick, amount)
                    onDismiss()
                })
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun <T> SearchStep(
    spec: CatalogPickerSpec<T>,
    query: String,
    onQuery: (String) -> Unit,
    search: suspend (String) -> List<T>,
    onPick: (T) -> Unit,
    onSaveText: () -> Unit,
) {
    val c = Baoji.colors
    val focus = remember { FocusRequester() }
    var results by remember { mutableStateOf(emptyList<T>()) }
    LaunchedEffect(query) {
        delay(SEARCH_DEBOUNCE_MS)
        results = search(query)
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        placeholder = { Text(spec.searchHint, color = c.secondaryText) },
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        colors = baojiFieldColors(),
    )
    Spacer(Modifier.height(8.dp))
    if (results.isEmpty()) {
        Text(
            if (query.isBlank()) spec.emptyHint else "内置库里没有找到",
            style = MaterialTheme.typography.bodyMedium,
            color = c.secondaryText,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
        // No keys: the source table has a few rows that share a name.
        items(results) { item ->
            Column(
                Modifier.fillMaxWidth().pressable(label = spec.itemTitle(item)) { onPick(item) }.padding(vertical = 10.dp),
            ) {
                Text(spec.itemTitle(item), style = MaterialTheme.typography.bodyLarge, color = c.primaryText)
                Text(spec.itemSubtitle(item), style = MaterialTheme.typography.bodyMedium, color = c.secondaryText)
            }
        }
    }
    if (query.isNotBlank()) {
        QuietButton("只记文字「${query.trim()}」", onClick = onSaveText)
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun <T> AmountStep(spec: CatalogPickerSpec<T>, item: T, onBack: () -> Unit, onSave: (Double) -> Unit) {
    val c = Baoji.colors
    val focus = remember { FocusRequester() }
    var text by rememberSaveable { mutableStateOf(spec.defaultAmount) }
    val amount = text.toDoubleOrNull()?.takeIf { it > 0 && it <= spec.maxAmount }
    Text(spec.itemTitle(item), style = MaterialTheme.typography.titleLarge, color = c.primaryText)
    Text(spec.itemSubtitle(item), style = MaterialTheme.typography.bodyMedium, color = c.secondaryText)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = text,
        onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' }.take(7) },
        singleLine = true,
        suffix = { Text(spec.amountUnit, color = c.secondaryText) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        colors = baojiFieldColors(),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        amount?.let { spec.preview(item, it) } ?: "请输入 0 到 ${spec.maxAmount.trimmed()} 之间的数",
        style = MaterialTheme.typography.bodyMedium,
        color = c.secondaryText,
    )
    Spacer(Modifier.height(16.dp))
    SolidButton("记下", Modifier.fillMaxWidth(), enabled = amount != null) { amount?.let(onSave) }
    QuietButton("换一个", onClick = onBack)
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun <T> SetsStep(spec: CatalogPickerSpec<T>, item: T, onBack: () -> Unit, onSave: (SetsEntry) -> Unit) {
    val c = Baoji.colors
    val focus = remember { FocusRequester() }
    var sets by rememberSaveable { mutableStateOf(DEFAULT_SETS) }
    var reps by rememberSaveable { mutableStateOf(DEFAULT_REPS) }
    var weight by rememberSaveable { mutableStateOf("") }
    val setCount = sets.toIntOrNull()?.takeIf { it in 1..MAX_SETS }
    val repCount = reps.toIntOrNull()?.takeIf { it in 1..MAX_REPS }
    // Empty weight is bodyweight; anything typed must be a sensible load.
    val load = if (weight.isEmpty()) null else weight.toDoubleOrNull()?.takeIf { it > 0 && it <= MAX_LIFT_KG }
    val valid = setCount != null && repCount != null && (weight.isEmpty() || load != null)
    Text(spec.itemTitle(item), style = MaterialTheme.typography.titleLarge, color = c.primaryText)
    Text(spec.itemSubtitle(item), style = MaterialTheme.typography.bodyMedium, color = c.secondaryText)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(sets, { sets = it.filter(Char::isDigit).take(2) }, "组", KeyboardType.Number, Modifier.weight(1f).focusRequester(focus))
        NumberField(reps, { reps = it.filter(Char::isDigit).take(3) }, "次", KeyboardType.Number, Modifier.weight(1f))
        NumberField(
            weight, { v -> weight = v.filter { it.isDigit() || it == '.' }.take(5) }, "kg", KeyboardType.Decimal,
            Modifier.weight(1.2f), placeholder = "自重",
        )
    }
    Spacer(Modifier.height(8.dp))
    Text(
        if (valid) spec.setsPreview(item, setCount!!) else "组数 1–$MAX_SETS，次数 1–$MAX_REPS，重量可空着表示自重",
        style = MaterialTheme.typography.bodyMedium,
        color = c.secondaryText,
    )
    Spacer(Modifier.height(16.dp))
    SolidButton("记下", Modifier.fillMaxWidth(), enabled = valid) {
        if (setCount != null && repCount != null) onSave(SetsEntry(setCount, repCount, load))
    }
    QuietButton("换一个", onClick = onBack)
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun NumberField(
    value: String,
    onValue: (String) -> Unit,
    unit: String,
    keyboard: KeyboardType,
    modifier: Modifier,
    placeholder: String? = null,
) {
    val c = Baoji.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        singleLine = true,
        placeholder = placeholder?.let { { Text(it, color = c.secondaryText) } },
        suffix = { Text(unit, color = c.secondaryText) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier,
        colors = baojiFieldColors(),
    )
}

/** Whole-number sheet, e.g. today's steps. Saving an empty field or 0 clears the value. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberSheet(title: String, unit: String, initial: Int?, max: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val c = Baoji.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focus = remember { FocusRequester() }
    var text by rememberSaveable { mutableStateOf(initial?.takeIf { it > 0 }?.toString() ?: "") }
    val value = if (text.isEmpty()) 0 else text.toIntOrNull()?.takeIf { it <= max }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = c.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = PagePadding).imePadding().navigationBarsPadding()) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.primaryText)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() }.take(max.toString().length) },
                singleLine = true,
                suffix = { Text(unit, color = c.secondaryText) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                colors = baojiFieldColors(),
            )
            Spacer(Modifier.height(16.dp))
            SolidButton("记下", Modifier.fillMaxWidth(), enabled = value != null) {
                value?.let(onSave)
                onDismiss()
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

private const val SEARCH_DEBOUNCE_MS = 120L
private const val DEFAULT_SETS = "3"
private const val DEFAULT_REPS = "10"
private const val MAX_SETS = 20
private const val MAX_REPS = 100
private const val MAX_LIFT_KG = 500.0
