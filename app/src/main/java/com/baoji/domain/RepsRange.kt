package com.baoji.domain

data class RepsRange(val min: Int, val max: Int) {
    init {
        require(min in 1..max) { "invalid reps range $min..$max" }
    }
}
