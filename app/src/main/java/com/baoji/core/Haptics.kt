package com.baoji.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.baoji.data.prefs.SettingsStore
import com.baoji.data.prefs.VibrationLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

enum class HapticKind { LIGHT, MEDIUM, HEAVY, DAY_COMPLETE }

/**
 * All vibration in the app. Strength follows the in-app setting; nothing fires when the
 * system haptic feedback switch is off. Devices without amplitude control get duration only.
 */
@Singleton
class Haptics @Inject constructor(
    @ApplicationContext private val context: Context,
    settings: SettingsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val level = settings.vibration.stateIn(scope, SharingStarted.Eagerly, VibrationLevel.STANDARD)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun play(kind: HapticKind) {
        val v = vibrator ?: return
        val lvl = level.value
        if (lvl == VibrationLevel.OFF || !v.hasVibrator() || !systemHapticsEnabled()) return
        val pattern = HapticPatterns.pattern(kind, lvl)
        val effect = if (v.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(pattern.timingsWithoutAmplitude(), -1)
        }
        v.vibrate(effect)
    }

    private fun systemHapticsEnabled(): Boolean =
        Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
}

/** Pure pattern table so it can be unit-tested. timings[0] is the initial off delay. */
data class HapticPattern(val timings: LongArray, val amplitudes: IntArray) {
    /** Without amplitude control, a stronger level is expressed as a longer pulse. */
    fun timingsWithoutAmplitude(): LongArray = timings.mapIndexed { i, t ->
        if (amplitudes[i] == 0) t else (t * (0.6 + amplitudes[i] / 255.0)).toLong()
    }.toLongArray()

    override fun equals(other: Any?) =
        other is HapticPattern && timings.contentEquals(other.timings) && amplitudes.contentEquals(other.amplitudes)

    override fun hashCode() = 31 * timings.contentHashCode() + amplitudes.contentHashCode()
}

object HapticPatterns {
    private fun scale(level: VibrationLevel): Double = when (level) {
        VibrationLevel.OFF -> 0.0
        VibrationLevel.LIGHT -> 0.5
        VibrationLevel.STANDARD -> 0.8
        VibrationLevel.STRONG -> 1.0
    }

    private fun amp(base: Int, level: VibrationLevel) = (base * scale(level)).toInt().coerceIn(1, 255)

    fun pattern(kind: HapticKind, level: VibrationLevel): HapticPattern = when (kind) {
        HapticKind.LIGHT -> HapticPattern(longArrayOf(0, 18), intArrayOf(0, amp(120, level)))
        HapticKind.MEDIUM -> HapticPattern(longArrayOf(0, 35), intArrayOf(0, amp(190, level)))
        HapticKind.HEAVY -> HapticPattern(longArrayOf(0, 70), intArrayOf(0, amp(255, level)))
        HapticKind.DAY_COMPLETE -> HapticPattern(
            longArrayOf(0, 30, 110, 30, 110, 180),
            intArrayOf(0, amp(170, level), 0, amp(170, level), 0, amp(255, level)),
        )
    }
}
