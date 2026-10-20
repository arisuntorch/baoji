package com.baoji.feature.training

import com.baoji.data.db.SetLogEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SetSummaryTest {

    private fun set(weight: Double, reps: Int) = SetLogEntity("s", "e", "卧推", 0, 0, weight, reps, 8.0, 0)

    @Test
    fun identicalSetsCollapse() {
        assertThat(setSummary(List(3) { set(80.0, 5) })).isEqualTo("80kg × 5 · 3 组")
    }

    @Test
    fun differentSetsListInOrder() {
        assertThat(setSummary(listOf(set(60.0, 8), set(62.5, 6)))).isEqualTo("60kg×8  62.5kg×6")
    }

    @Test
    fun zeroWeightReadsAsBodyweight() {
        assertThat(setSummary(listOf(set(0.0, 12)))).isEqualTo("自重 × 12 · 1 组")
    }
}
