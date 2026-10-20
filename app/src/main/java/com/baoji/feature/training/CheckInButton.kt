package com.baoji.feature.training

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.baoji.core.theme.Baoji
import com.baoji.core.ui.pressable
import com.baoji.core.ui.rememberReduceMotion

/** Slowly breathing round "开始" button. No timer appears after it is pressed. */
@Composable
fun CheckInButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Baoji.colors
    val reduce = rememberReduceMotion()
    val breath = rememberInfiniteTransition(label = "breath")
    val phase by breath.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "phase",
    )
    val p = if (reduce) 0.5f else phase
    Box(modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(132.dp)
                .graphicsLayer {
                    scaleX = 0.88f + 0.12f * p
                    scaleY = 0.88f + 0.12f * p
                    alpha = 0.25f + 0.25f * (1 - p)
                }
                .clip(CircleShape)
                .background(c.fill),
        )
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(c.card)
                .border(1.5.dp, c.primaryText, CircleShape)
                .pressable(label = "开始训练", onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text("开始", style = MaterialTheme.typography.titleMedium, color = c.primaryText)
        }
    }
}
