package com.baoji.core

import com.baoji.core.icon.FoodIcon
import com.baoji.core.icon.IconMatcher
import com.baoji.core.icon.LineIcons
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
        assertThat(m.match("平板支撑")).isEqualTo(TrainingIcon.PLANK)
        assertThat(m.match("太极")).isEqualTo(TrainingIcon.DEFAULT)
    }

    @Test
    fun newTrainingKeywords() {
        val m = IconMatcher.training
        assertThat(m.match("哑铃侧平举")).isEqualTo(TrainingIcon.LATERAL_RAISE)
        assertThat(m.match("绳索面拉")).isEqualTo(TrainingIcon.FACE_PULL)
        assertThat(m.match("绳索下压")).isEqualTo(TrainingIcon.PUSHDOWN)
        assertThat(m.match("仰卧臂屈伸")).isEqualTo(TrainingIcon.PUSHDOWN)
        assertThat(m.match("双杠臂屈伸")).isEqualTo(TrainingIcon.DIP)
        assertThat(m.match("引体向上")).isEqualTo(TrainingIcon.PULL_UP)
        assertThat(m.match("坐姿腿弯举")).isEqualTo(TrainingIcon.LEG_CURL)
        assertThat(m.match("腿屈伸")).isEqualTo(TrainingIcon.LEG_EXTENSION)
        assertThat(m.match("站姿提踵")).isEqualTo(TrainingIcon.CALF_RAISE)
        assertThat(m.match("杠铃臀推")).isEqualTo(TrainingIcon.HIP_THRUST)
        assertThat(m.match("保加利亚分腿蹲")).isEqualTo(TrainingIcon.LUNGE)
        assertThat(m.match("卷腹")).isEqualTo(TrainingIcon.CRUNCH)
        assertThat(m.match("俯卧撑")).isEqualTo(TrainingIcon.PUSH_UP)
        assertThat(m.match("跳绳")).isEqualTo(TrainingIcon.JUMP_ROPE)
        assertThat(m.match("动感单车")).isEqualTo(TrainingIcon.CYCLE)
        assertThat(m.match("游泳")).isEqualTo(TrainingIcon.SWIM)
        assertThat(m.match("壶铃摇摆")).isEqualTo(TrainingIcon.KETTLEBELL)
        assertThat(m.match("拉伸")).isEqualTo(TrainingIcon.STRETCH)
        assertThat(m.match("Overhead Press")).isEqualTo(TrainingIcon.PRESS)
        assertThat(m.match("Lateral Raise")).isEqualTo(TrainingIcon.LATERAL_RAISE)
    }

    @Test
    fun everyIconHasADrawing() {
        TrainingIcon.entries.forEach { assertThat(LineIcons.training(it).root.size).isGreaterThan(0) }
        FoodIcon.entries.forEach { assertThat(LineIcons.food(it).root.size).isGreaterThan(0) }
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

    @Test
    fun newFoodKeywords() {
        val m = IconMatcher.food
        assertThat(m.match("希腊酸奶")).isEqualTo(FoodIcon.YOGURT)
        assertThat(m.match("奶酪")).isEqualTo(FoodIcon.CHEESE)
        assertThat(m.match("牛奶")).isEqualTo(FoodIcon.MILK)
        assertThat(m.match("虾仁")).isEqualTo(FoodIcon.SHRIMP)
        assertThat(m.match("北豆腐")).isEqualTo(FoodIcon.SOY)
        assertThat(m.match("无糖豆浆")).isEqualTo(FoodIcon.SOY)
        assertThat(m.match("蒸土豆")).isEqualTo(FoodIcon.POTATO)
        assertThat(m.match("烤红薯")).isEqualTo(FoodIcon.POTATO)
        assertThat(m.match("玉米")).isEqualTo(FoodIcon.CORN)
        assertThat(m.match("苹果")).isEqualTo(FoodIcon.APPLE)
        assertThat(m.match("水果")).isEqualTo(FoodIcon.APPLE)
        assertThat(m.match("牛油果")).isEqualTo(FoodIcon.AVOCADO)
        assertThat(m.match("混合坚果")).isEqualTo(FoodIcon.NUTS)
        assertThat(m.match("美式咖啡")).isEqualTo(FoodIcon.COFFEE)
        assertThat(m.match("水")).isEqualTo(FoodIcon.WATER)
        assertThat(m.match("猪肉水饺")).isEqualTo(FoodIcon.DUMPLING)
        assertThat(m.match("鸡蛋")).isEqualTo(FoodIcon.EGG)
        assertThat(m.match("鸡蛋饼")).isEqualTo(FoodIcon.EGG)
        assertThat(m.match("鸡胸")).isEqualTo(FoodIcon.CHICKEN)
        assertThat(m.match("蛋白粉奶昔")).isEqualTo(FoodIcon.PROTEIN)
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
