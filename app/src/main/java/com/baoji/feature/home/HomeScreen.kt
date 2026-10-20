package com.baoji.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.CardShape
import com.baoji.core.ui.PagePadding
import com.baoji.core.ui.RollingNumber
import com.baoji.core.ui.Watermark
import com.baoji.core.ui.pressable

@Composable
fun HomeScreen(onOpenDiary: () -> Unit, onOpenWeight: () -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return
    HomeContent(s, onOpenDiary, onOpenWeight)
}

@Composable
fun HomeContent(state: HomeState, onOpenDiary: () -> Unit, onOpenWeight: () -> Unit) {
    val c = Baoji.colors
    Box(Modifier.fillMaxSize()) {
        Watermark(if (state.isTrainingDay) "训练日" else "休息日")
        Column(
            Modifier.fillMaxSize().padding(horizontal = PagePadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Sit the figures in the upper part of the screen rather than dead centre.
            Spacer(Modifier.weight(0.55f))
            BigFigure("摄入", state.intake)
            Box(Modifier.padding(vertical = 20.dp).width(160.dp).height(0.5.dp).background(c.separator))
            BigFigure("消耗", state.burned)
            Spacer(Modifier.weight(1.45f))
            Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EntryButton("日记", Modifier.weight(1f), onOpenDiary)
                EntryButton("体重", Modifier.weight(1f), onOpenWeight)
            }
        }
    }
}

@Composable
private fun BigFigure(label: String, value: Long?) {
    val c = Baoji.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label ${value ?: "—"} 千卡"
        },
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.secondaryText)
        Spacer(Modifier.height(4.dp))
        if (value == null) {
            Text("—", style = MaterialTheme.typography.displayLarge, color = c.primaryText)
        } else {
            RollingNumber(value, MaterialTheme.typography.displayLarge, c.primaryText)
        }
    }
}

@Composable
private fun EntryButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    val c = Baoji.colors
    Box(
        modifier
            .height(52.dp)
            .clip(CardShape)
            .background(c.card)
            .pressable(label = text, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.primaryText)
    }
}
