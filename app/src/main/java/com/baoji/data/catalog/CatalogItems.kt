package com.baoji.data.catalog

import com.baoji.data.repo.Macros
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One food from the built-in table. Nutrients are per 100 g edible portion. */
@Serializable
data class FoodItem(
    @SerialName("n") val name: String,
    @SerialName("c") val category: String,
    @SerialName("k") val kcal: Double,
    @SerialName("p") val protein: Double,
    @SerialName("f") val fat: Double,
    @SerialName("h") val carbs: Double,
) {
    fun macrosFor(grams: Double): Macros {
        val r = grams.coerceAtLeast(0.0) / 100.0
        return Macros(protein = protein * r, carbs = carbs * r, fat = fat * r, kcal = kcal * r)
    }
}

/**
 * One activity from the built-in list with its MET value (Compendium of Physical Activities).
 * [strength] moves such as 卧推 are logged by sets × reps × weight; the rest by minutes.
 */
@Serializable
data class ExerciseItem(
    @SerialName("n") val name: String,
    @SerialName("c") val category: String,
    @SerialName("m") val met: Double,
    @SerialName("s") val strength: Boolean = false,
)
