package com.baoji.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baoji.core.theme.Baoji
import kotlinx.coroutines.delay

/**
 * Swipe right to delete. Only left-to-right is enabled so a stray leftward swipe never deletes.
 * Screen readers get the same action as a custom "删除" action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDelete(label: String, onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val delete by rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState(positionalThreshold = { total -> total * 0.4f })
    // Fire once the row has settled off-screen; confirmValueChange can be called more than once per swipe.
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            delete()
            // Normally the row leaves the list and this effect is cancelled; if it is still here, slide it back.
            delay(SETTLE_MS)
            state.reset()
        }
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        modifier = modifier.semantics {
            customActions = listOf(CustomAccessibilityAction("删除$label") { delete(); true })
        },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(CardShape).background(Baoji.colors.fill).padding(start = 24.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("删除", style = MaterialTheme.typography.labelLarge, color = Baoji.colors.primaryText)
            }
        },
    ) { content() }
}

private const val SETTLE_MS = 800L
