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
        assertThat(MuscleMap.workFor("Machine Chest Fly").primary).containsExactly(Muscle.CHEST)
    }

    @Test
    fun unknownExerciseWorksNothing() {
        assertThat(MuscleMap.workFor("太极").primary).isEmpty()
        assertThat(MuscleMap.levels(listOf(ExerciseLoad("太极", sets = 3, done = 3)))).isEmpty()
    }

    @Test
    fun plannedShareScalesBySetsAndRole() {
        // Bench 4 sets: chest 4, shoulders/triceps 2. Fly 2 sets: chest +2, shoulders +1.
        val levels = MuscleMap.levels(listOf(ExerciseLoad("卧推", 4, 0), ExerciseLoad("飞鸟", 2, 0)))
        assertThat(levels.getValue(Muscle.CHEST).planned).isEqualTo(1f)
        assertThat(levels.getValue(Muscle.SHOULDERS).planned).isWithin(1e-6f).of(0.5f)
        assertThat(levels.getValue(Muscle.TRICEPS).planned).isWithin(1e-6f).of(2f / 6f)
        assertThat(levels).doesNotContainKey(Muscle.QUADS)
    }

    @Test
    fun nothingDoneBeforeTraining() {
        val levels = MuscleMap.levels(listOf(ExerciseLoad("深蹲", 4, 0)))
        assertThat(levels.values.all { it.done == 0f }).isTrue()
    }

    @Test
    fun doneGrowsWithEachSet() {
        val half = MuscleMap.levels(listOf(ExerciseLoad("深蹲", 4, 2))).getValue(Muscle.QUADS).done
        val full = MuscleMap.levels(listOf(ExerciseLoad("深蹲", 4, 4))).getValue(Muscle.QUADS).done
        assertThat(half).isWithin(1e-6f).of(0.5f)
        assertThat(full).isEqualTo(1f)
    }

    @Test
    fun finishedHelperMuscleStaysLighterThanFinishedMainMuscle() {
        val levels = MuscleMap.levels(listOf(ExerciseLoad("卧推", 4, 4)))
        assertThat(levels.getValue(Muscle.CHEST).done).isEqualTo(1f)
        assertThat(levels.getValue(Muscle.TRICEPS).done).isWithin(1e-6f).of(0.5f)
    }

    @Test
    fun extraSetsBeyondPlanDoNotOverflow() {
        val quads = MuscleMap.levels(listOf(ExerciseLoad("深蹲", 3, 5))).getValue(Muscle.QUADS)
        assertThat(quads.done).isEqualTo(1f)
    }

    @Test
    fun eachMuscleDeepensWithItsOwnProgress() {
        // Lots of shoulder work planned; finishing the small chest share must still show clearly.
        val loads = listOf(ExerciseLoad("上斜卧推", 3, 3), ExerciseLoad("侧平举", 4, 0), ExerciseLoad("面拉", 3, 0))
        val chest = MuscleMap.levels(loads).getValue(Muscle.CHEST)
        assertThat(chest.depth).isAtLeast(0.5f)
        val shoulders = MuscleMap.levels(loads).getValue(Muscle.SHOULDERS)
        assertThat(shoulders.depth).isLessThan(chest.depth)
    }

    @Test
    fun depthReachesFullOnlyForTheMostWorkedMuscleWhenFinished() {
        val levels = MuscleMap.levels(listOf(ExerciseLoad("卧推", 4, 4)))
        assertThat(levels.getValue(Muscle.CHEST).depth).isEqualTo(1f)
        assertThat(levels.getValue(Muscle.TRICEPS).depth).isWithin(1e-6f).of(0.75f)
        assertThat(MuscleMap.levels(listOf(ExerciseLoad("卧推", 4, 2))).getValue(Muscle.CHEST).depth).isWithin(1e-6f).of(0.5f)
    }

    @Test
    fun zeroSetsCountForNothing() {
        assertThat(MuscleMap.levels(listOf(ExerciseLoad("深蹲", 0, 0)))).isEmpty()
    }
}
