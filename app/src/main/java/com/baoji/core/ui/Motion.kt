package com.baoji.core.ui

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** True when the user turned off animations (animator duration scale = 0). */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

fun <T> snapSpec(): AnimationSpec<T> = snap()

/** Picks [normal] unless motion is reduced. */
fun <T> motion(reduce: Boolean, normal: AnimationSpec<T>): AnimationSpec<T> = if (reduce) snap() else normal
