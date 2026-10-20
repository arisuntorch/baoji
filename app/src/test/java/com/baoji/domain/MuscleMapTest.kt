package com.baoji.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MuscleMapTest {

    @Test
    fun squatWorksLegsMostly() {
        val work = MuscleMap.workFor("深蹲")
        assertThat(work.primary).containsExactly(Muscle.QUADS, Muscle.GLUTES)
        assertThat(work.secondary).contains(Muscle.HAMSTRINGS)
    }

    @Test
    fun longerKeywordWins() {
        // "腿弯举" must not be read as the biceps "弯举".
        assertThat(MuscleMap.workFor("坐姿腿弯举").primary).containsExactly(Muscle.HAMSTRINGS)
        assertThat(MuscleMap.workFor("哑铃弯举").primary).containsExactly(Muscle.BICEPS)
        assertThat(MuscleMap.workFor("Leg Press").primary).contains(Muscle.QUADS)
        assertThat(MuscleMap.workFor("Bench Press").primary).containsExactly(Muscle.CHEST)
    }

    @Test
    fun unknownExerciseWorksNothing() {
        assertThat(MuscleMap.workFor("太极").primary).isEmpty()
        assertThat(MuscleMap.levels(listOf("太极" to 3))).isEmpty()
    }

    @Test
    fun levelsScaleBySetsAndRole() {
        // Bench 4 sets: chest 4, shoulders/triceps 2. Fly 2 sets: chest +2, shoulders +1.
        val levels = MuscleMap.levels(listOf("卧推" to 4, "飞鸟" to 2))
        assertThat(levels[Muscle.CHEST]).isEqualTo(1f)
        assertThat(levels[Muscle.SHOULDERS]).isWithin(1e-6f).of(0.5f)
        assertThat(levels[Muscle.TRICEPS]).isWithin(1e-6f).of(2f / 6f)
        assertThat(levels).doesNotContainKey(Muscle.QUADS)
    }

    @Test
    fun zeroSetsCountForNothing() {
        assertThat(MuscleMap.levels(listOf("深蹲" to 0))).isEmpty()
    }
}
