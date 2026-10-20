package com.baoji.domain

/**
 * 加重标记: the last session of an exercise hit the top of the rep range on every planned set.
 */
object OverloadRule {
    fun shouldMark(lastSessionReps: List<Int>, plannedSets: Int, repsMax: Int): Boolean {
        if (plannedSets <= 0 || lastSessionReps.size < plannedSets) return false
        return lastSessionReps.all { it >= repsMax }
    }
}
