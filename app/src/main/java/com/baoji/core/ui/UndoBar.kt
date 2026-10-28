package com.baoji.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baoji.core.theme.Baoji
import kotlinx.coroutines.delay

/** The last swipe-delete, kept for a few seconds so a quick flick can be taken back. */
class Undo(val label: String, val restore: suspend () -> Unit)

/** Small quiet capsule at the bottom: "已删除「x」 撤销". Disappears on its own. */
@Composable
fun BoxScope.UndoBar(undo: Undo?, onUndo: () -> Unit, onTimeout: () -> Unit) {
    if (undo == null) return
    LaunchedEffect(undo) {
        delay(UNDO_VISIBLE_MS)
        onTimeout()
    }
    val c = Baoji.colors
    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = PagePadding, vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(CircleShape).background(c.primaryText).padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "已删除「${undo.label}」",
                style = MaterialTheme.typography.bodyMedium,
                color = c.background,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.pressable(label = "撤销删除", onClick = onUndo).padding(horizontal = 12.dp, vertical = 14.dp)) {
                Text("撤销", style = MaterialTheme.typography.labelLarge, color = c.background)
            }
        }
    }
}

private const val UNDO_VISIBLE_MS = 4_000L
