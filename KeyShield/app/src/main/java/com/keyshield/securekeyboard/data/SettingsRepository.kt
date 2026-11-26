package com.keyshield.securekeyboard.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.keyshield.securekeyboard.core.RandomizationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "settings")

data class KeyShieldSettings(
    val randomization: RandomizationSettings = RandomizationSettings(),
    val haptics: Boolean = true,
)

/** Stores preference switches only. Typed content never reaches this class. */
class SettingsRepository(private val context: Context) {
    val settings: Flow<KeyShieldSettings> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            KeyShieldSettings(
                randomization = RandomizationSettings(
                    adaptiveEnabled = p[ADAPTIVE] ?: true,
                    randomizeNumbers = p[NUMBERS] ?: true,
                    randomizePhone = p[PHONE] ?: false,
                ),
                haptics = p[HAPTICS] ?: true,
            )
        }

    suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }

    companion object {
        val ADAPTIVE = booleanPreferencesKey("adaptive_enabled")
        val NUMBERS = booleanPreferencesKey("randomize_numbers")
        val PHONE = booleanPreferencesKey("randomize_phone")
        val HAPTICS = booleanPreferencesKey("haptics")
    }
}
