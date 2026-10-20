package com.baoji.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick as semanticsClick
import androidx.compose.ui.semantics.onLongClick as semanticsLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/** Gentle spring, no visible overshoot. */
fun <T> softSpring() = spring<T>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)

/**
 * Press feedback without ripples: scale to 96% and dim slightly, spring back on release.
 * Falls back to an instant state change when system animations are off.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    label: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val reduce = rememberReduceMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = if (reduce) snapSpec() else softSpring(),
        label = "pressScale",
    )
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (pressed) 0.7f else 1f
        }
        .semantics {
            role = Role.Button
            if (!enabled) disabled()
            semanticsClick(label = label) { click(); true }
            if (longClick != null) semanticsLongClick { longClick?.invoke(); true }
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                },
                onTap = { click() },
                onLongPress = longClick?.let { { _ -> it() } },
            )
        }
}
