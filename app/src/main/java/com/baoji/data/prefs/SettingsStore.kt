package com.baoji.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class NutritionTargets(val protein: Double, val carbs: Double, val fat: Double) {
    val kcal: Double get() = 4 * protein + 4 * carbs + 9 * fat

    companion object {
        val EMPTY = NutritionTargets(0.0, 0.0, 0.0)
    }
}

enum class VibrationLevel { OFF, LIGHT, STANDARD, STRONG }

@Singleton
class SettingsStore @Inject constructor(private val store: DataStore<Preferences>) {

    val targets: Flow<NutritionTargets> = store.data.map { p ->
        NutritionTargets(p[PROTEIN] ?: 0.0, p[CARBS] ?: 0.0, p[FAT] ?: 0.0)
    }

    val vibration: Flow<VibrationLevel> = store.data.map { p ->
        p[VIBRATION]?.let { runCatching { VibrationLevel.valueOf(it) }.getOrNull() } ?: VibrationLevel.STANDARD
    }

    suspend fun currentTargets(): NutritionTargets = targets.first()

    suspend fun setTargets(t: NutritionTargets) {
        store.edit {
            it[PROTEIN] = t.protein
            it[CARBS] = t.carbs
            it[FAT] = t.fat
        }
    }

    suspend fun setVibration(level: VibrationLevel) {
        store.edit { it[VIBRATION] = level.name }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    private companion object {
        val PROTEIN = doublePreferencesKey("target_protein")
        val CARBS = doublePreferencesKey("target_carbs")
        val FAT = doublePreferencesKey("target_fat")
        val VIBRATION = stringPreferencesKey("vibration")
    }
}
