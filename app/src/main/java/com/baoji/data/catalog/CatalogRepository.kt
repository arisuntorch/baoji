package com.baoji.data.catalog

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Read-only reference tables shipped in assets/catalog. Loaded once on first search. */
@Singleton
class CatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val lock = Mutex()
    private var foods: List<FoodItem>? = null
    private var exercises: List<ExerciseItem>? = null

    suspend fun searchFoods(query: String): List<FoodItem> =
        CatalogSearch.search(allFoods(), query) { it.name }

    suspend fun searchExercises(query: String): List<ExerciseItem> =
        CatalogSearch.search(allExercises(), query) { "${it.name} ${it.category}" }

    suspend fun allExercises(): List<ExerciseItem> = lock.withLock {
        exercises ?: load(EXERCISES, ExerciseItem.serializer()).also { if (it.isNotEmpty()) exercises = it }
    }

    private suspend fun allFoods(): List<FoodItem> = lock.withLock {
        foods ?: load(FOODS, FoodItem.serializer()).also { if (it.isNotEmpty()) foods = it }
    }

    private suspend fun <T> load(path: String, item: KSerializer<T>): List<T> = withContext(Dispatchers.IO) {
        try {
            val text = context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSON.decodeFromString(ListSerializer(item), text)
        } catch (e: IOException) {
            Log.e(TAG, "Cannot read $path", e)
            emptyList()
        } catch (e: SerializationException) {
            Log.e(TAG, "Malformed $path", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "Catalog"
        const val FOODS = "catalog/foods.json"
        const val EXERCISES = "catalog/exercises.json"
        private val JSON = Json { ignoreUnknownKeys = true }
    }
}
