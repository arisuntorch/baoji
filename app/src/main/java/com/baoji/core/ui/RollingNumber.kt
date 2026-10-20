package com.baoji.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle

/**
 * Each digit rolls on its own: a digit that went up slides in from below, down from above.
 * Digits are aligned from the right so the units column stays put when the length changes.
 */
@Composable
fun RollingNumber(value: Long, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val text = value.toString()
    val reduce = rememberReduceMotion()
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = text }) {
        text.forEachIndexed { index, ch ->
            val place = text.length - index
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    if (reduce) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        val up = targetState > initialState
                        (slideInVertically(tween(260)) { h -> if (up) h else -h } + fadeIn(tween(260))) togetherWith
                            (slideOutVertically(tween(260)) { h -> if (up) -h else h } + fadeOut(tween(200)))
                    }
                },
                label = "digit$place",
            ) { digit ->
                Text(digit.toString(), style = style, color = color)
            }
        }
    }
}
