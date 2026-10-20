package com.baoji.domain

import com.baoji.core.icon.IconMatcher

enum class Muscle {
    TRAPS, SHOULDERS, CHEST, BICEPS, TRICEPS, FOREARMS, ABS, OBLIQUES,
    LATS, LOWER_BACK, GLUTES, QUADS, ADDUCTORS, HAMSTRINGS, CALVES,
}

/** Which muscles an exercise mainly works and which it only helps with. */
data class MuscleWork(val primary: Set<Muscle>, val secondary: Set<Muscle> = emptySet()) {
    companion object {
        val NONE = MuscleWork(emptySet())
    }
}

/** Exercise name → muscles, by the same longest-keyword match as the icons. */
object MuscleMap {

    private const val SECONDARY_SHARE = 0.5f

    private fun work(primary: Set<Muscle>, secondary: Set<Muscle> = emptySet()) = MuscleWork(primary, secondary)

    private val bench = work(setOf(Muscle.CHEST), setOf(Muscle.SHOULDERS, Muscle.TRICEPS))
    private val fly = work(setOf(Muscle.CHEST), setOf(Muscle.SHOULDERS))
    private val squat = work(setOf(Muscle.QUADS, Muscle.GLUTES), setOf(Muscle.ADDUCTORS, Muscle.HAMSTRINGS, Muscle.LOWER_BACK))
    private val lunge = work(setOf(Muscle.QUADS, Muscle.GLUTES), setOf(Muscle.HAMSTRINGS, Muscle.ADDUCTORS))
    private val legPress = work(setOf(Muscle.QUADS, Muscle.GLUTES), setOf(Muscle.ADDUCTORS, Muscle.HAMSTRINGS))
    private val legExtension = work(setOf(Muscle.QUADS))
    private val legCurl = work(setOf(Muscle.HAMSTRINGS), setOf(Muscle.CALVES))
    private val deadlift = work(
        setOf(Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.LOWER_BACK),
        setOf(Muscle.TRAPS, Muscle.QUADS, Muscle.FOREARMS, Muscle.LATS),
    )
    private val rdl = work(setOf(Muscle.HAMSTRINGS, Muscle.GLUTES), setOf(Muscle.LOWER_BACK))
    private val hipThrust = work(setOf(Muscle.GLUTES), setOf(Muscle.HAMSTRINGS))
    private val overheadPress = work(setOf(Muscle.SHOULDERS), setOf(Muscle.TRICEPS, Muscle.TRAPS))
    private val lateralRaise = work(setOf(Muscle.SHOULDERS))
    private val rearDelt = work(setOf(Muscle.SHOULDERS), setOf(Muscle.TRAPS))
    private val shrug = work(setOf(Muscle.TRAPS))
    private val row = work(setOf(Muscle.LATS), setOf(Muscle.TRAPS, Muscle.BICEPS, Muscle.SHOULDERS))
    private val pulldown = work(setOf(Muscle.LATS), setOf(Muscle.BICEPS, Muscle.FOREARMS))
    private val curl = work(setOf(Muscle.BICEPS), setOf(Muscle.FOREARMS))
    private val dip = work(setOf(Muscle.TRICEPS), setOf(Muscle.CHEST, Muscle.SHOULDERS))
    private val pushdown = work(setOf(Muscle.TRICEPS))
    private val core = work(setOf(Muscle.ABS), setOf(Muscle.OBLIQUES))
    private val twist = work(setOf(Muscle.OBLIQUES), setOf(Muscle.ABS))
    private val backExtension = work(setOf(Muscle.LOWER_BACK), setOf(Muscle.GLUTES, Muscle.HAMSTRINGS))
    private val calfRaise = work(setOf(Muscle.CALVES))
    private val wrist = work(setOf(Muscle.FOREARMS))
    private val cardio = work(emptySet(), setOf(Muscle.QUADS, Muscle.CALVES, Muscle.HAMSTRINGS))

    private val matcher = IconMatcher(
        mapOf(
            "卧推" to bench, "bench press" to bench, "bench" to bench, "胸推" to bench, "推胸" to bench,
            "俯卧撑" to bench, "push-up" to bench, "pushup" to bench,
            "飞鸟" to fly, "夹胸" to fly, "fly" to fly,
            "深蹲" to squat, "squat" to squat, "蹲" to squat,
            "箭步" to lunge, "弓步" to lunge, "lunge" to lunge,
            "腿举" to legPress, "leg press" to legPress,
            "腿屈伸" to legExtension, "leg extension" to legExtension,
            "腿弯举" to legCurl, "leg curl" to legCurl,
            "硬拉" to deadlift, "deadlift" to deadlift,
            "罗马尼亚" to rdl, "直腿硬拉" to rdl, "rdl" to rdl,
            "臀推" to hipThrust, "臀桥" to hipThrust, "hip thrust" to hipThrust,
            "推举" to overheadPress, "肩推" to overheadPress, "press" to overheadPress, "ohp" to overheadPress,
            "平举" to lateralRaise, "lateral raise" to lateralRaise,
            "面拉" to rearDelt, "face pull" to rearDelt, "反向飞鸟" to rearDelt, "reverse fly" to rearDelt,
            "耸肩" to shrug, "shrug" to shrug,
            "划船" to row, "row" to row,
            "下拉" to pulldown, "pulldown" to pulldown, "引体" to pulldown,
            "pull-up" to pulldown, "pullup" to pulldown, "chin-up" to pulldown,
            "弯举" to curl, "curl" to curl,
            "臂屈伸" to dip, "双杠" to dip, "dip" to dip,
            "下压" to pushdown, "pushdown" to pushdown, "三头" to pushdown,
            "卷腹" to core, "仰卧起坐" to core, "crunch" to core, "sit-up" to core,
            "平板支撑" to core, "plank" to core, "举腿" to core, "腹" to core,
            "转体" to twist, "侧桥" to twist, "twist" to twist,
            "山羊挺身" to backExtension, "背伸" to backExtension, "hyperextension" to backExtension,
            "提踵" to calfRaise, "calf" to calfRaise,
            "腕" to wrist, "wrist" to wrist,
            "跑" to cardio, "run" to cardio, "走" to cardio, "步行" to cardio, "walk" to cardio,
        ),
        MuscleWork.NONE,
    )

    fun workFor(exerciseName: String): MuscleWork = matcher.match(exerciseName)

    /**
     * How hard each muscle is worked today, 0..1 relative to the hardest-worked one.
     * Each planned set counts fully for its main muscles and half for the helpers.
     * Muscles with no work are absent from the map.
     */
    fun levels(exercises: List<Pair<String, Int>>): Map<Muscle, Float> {
        val score = mutableMapOf<Muscle, Float>()
        exercises.filter { it.second > 0 }.forEach { (name, sets) ->
            val w = workFor(name)
            w.primary.forEach { score.merge(it, sets.toFloat(), Float::plus) }
            w.secondary.forEach { score.merge(it, sets * SECONDARY_SHARE, Float::plus) }
        }
        val max = score.values.maxOrNull() ?: return emptyMap()
        return score.mapValues { it.value / max }
    }
}
