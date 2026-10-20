package com.baoji.domain

import java.time.LocalDate

object AgeCalculator {
    /** Age advances every Jan 1: enteredAge + (thisYear − enteredYear). Never below enteredAge. */
    fun currentAge(enteredAge: Int, enteredYear: Int, today: LocalDate): Int =
        enteredAge + maxOf(0, today.year - enteredYear)
}
