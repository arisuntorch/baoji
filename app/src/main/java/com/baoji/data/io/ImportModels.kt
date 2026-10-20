package com.baoji.data.io

/** Validated, normalized import content. Independent of both JSON shape and Room entities. */

data class ParsedActivity(val name: String, val kcal: Double)

data class ParsedExercise(
    val name: String,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val weight: Double,
    val kcalPerSet: Double,
)

data class ParsedPlan(val name: String, val days: List<Int>, val exercises: List<ParsedExercise>)

data class ParsedTraining(val daily: List<ParsedActivity>, val plans: List<ParsedPlan>)

data class ParsedTargets(val protein: Double, val carbs: Double, val fat: Double)

data class ParsedMeal(
    val name: String,
    val amount: Double,
    val unit: String,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val kcal: Double,
)

data class ParsedDiet(val targets: ParsedTargets?, val meals: List<ParsedMeal>)

data class ImportIssue(val line: Int, val message: String)

sealed interface ValidationResult<out T> {
    data class Ok<T>(val value: T) : ValidationResult<T>
    data class Invalid(val issues: List<ImportIssue>) : ValidationResult<Nothing>
}

enum class ImportKind(val wire: String) {
    TRAINING("training"), DIET("diet"), BACKUP("backup");

    companion object {
        fun fromWire(s: String): ImportKind? = entries.firstOrNull { it.wire == s }
    }
}

enum class ImportMode { MERGE, REPLACE }
