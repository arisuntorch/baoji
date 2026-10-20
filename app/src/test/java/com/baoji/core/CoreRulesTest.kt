package com.baoji.core

import com.baoji.core.icon.FoodIcon
import com.baoji.core.icon.IconMatcher
import com.baoji.core.icon.TrainingIcon
import com.baoji.data.prefs.VibrationLevel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IconMatcherTest {
    @Test
    fun trainingKeywords() {
        val m = IconMatcher.training
        assertThat(m.match("卧推")).isEqualTo(TrainingIcon.BENCH)
        assertThat(m.match("上斜哑铃卧推")).isEqualTo(TrainingIcon.BENCH)
        assertThat(m.match("Bench Press")).isEqualTo(TrainingIcon.BENCH)
        assertThat(m.match("高位下拉")).isEqualTo(TrainingIcon.PULLDOWN)
        assertThat(m.match("罗马尼亚硬拉")).isEqualTo(TrainingIcon.DEADLIFT)
        assertThat(m.match("腿举")).isEqualTo(TrainingIcon.LEG_PRESS)
        assertThat(m.match("步行去教室")).isEqualTo(TrainingIcon.WALK)
        assertThat(m.match("平板支撑")).isEqualTo(TrainingIcon.DEFAULT)
    }

    @Test
    fun longerKeywordWins() {
        assertThat(IconMatcher.food.match("蛋白粉")).isEqualTo(FoodIcon.PROTEIN)
        assertThat(IconMatcher.food.match("全麦面包")).isEqualTo(FoodIcon.BREAD)
        assertThat(IconMatcher.training.match("Leg Press")).isEqualTo(TrainingIcon.LEG_PRESS)
    }

    @Test
    fun foodKeywords() {
        val m = IconMatcher.food
        assertThat(m.match("练前鸡蛋")).isEqualTo(FoodIcon.EGG)
        assertThat(m.match("711 茶叶蛋")).isEqualTo(FoodIcon.EGG)
        assertThat(m.match("奶昔")).isEqualTo(FoodIcon.MILK)
        assertThat(m.match("鸡胸肉")).isEqualTo(FoodIcon.CHICKEN)
        assertThat(m.match("米饭")).isEqualTo(FoodIcon.RICE)
        assertThat(m.match("可乐")).isEqualTo(FoodIcon.DEFAULT)
    }
}

class HapticPatternsTest {
    @Test
    fun strongerLevelMeansHigherAmplitude() {
        val light = HapticPatterns.pattern(HapticKind.MEDIUM, VibrationLevel.LIGHT).amplitudes.max()
        val strong = HapticPatterns.pattern(HapticKind.MEDIUM, VibrationLevel.STRONG).amplitudes.max()
        assertThat(strong).isGreaterThan(light)
    }

    @Test
    fun dayCompleteIsShortShortLong() {
        val p = HapticPatterns.pattern(HapticKind.DAY_COMPLETE, VibrationLevel.STANDARD)
        val pulses = p.timings.filterIndexed { i, _ -> p.amplitudes[i] > 0 }
        assertThat(pulses).hasSize(3)
        assertThat(pulses[0]).isEqualTo(pulses[1])
        assertThat(pulses[2]).isGreaterThan(pulses[0])
    }

    @Test
    fun amplitudesStayInRange() {
        HapticKind.entries.forEach { k ->
            VibrationLevel.entries.filter { it != VibrationLevel.OFF }.forEach { l ->
                HapticPatterns.pattern(k, l).amplitudes.filter { it != 0 }.forEach { assertThat(it).isIn(1..255) }
            }
        }
    }

    @Test
    fun withoutAmplitudeControlStrongStretchesTiming() {
        val p = HapticPatterns.pattern(HapticKind.HEAVY, VibrationLevel.STRONG)
        assertThat(p.timingsWithoutAmplitude()[1]).isAtLeast(p.timings[1])
    }
}
