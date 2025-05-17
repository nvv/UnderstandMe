package com.vnamashko.understandme.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(private val context: Context) {

    val sourceLanguage: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[SOURCE_LANGUAGE] ?: context.resources.configuration.locales.get(0)?.language }

    val targetLanguage: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[TARGET_LANGUAGE] }

    val allowDataDownload: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ALLOW_DATA_DOWNLOAD] ?: false }

    suspend fun saveSourceLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[SOURCE_LANGUAGE] = language
        }
    }

    suspend fun saveTargetLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[TARGET_LANGUAGE] = language
        }
    }

    suspend fun saveAllowDataDownload(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALLOW_DATA_DOWNLOAD] = value
        }
    }

    companion object {
        private val SOURCE_LANGUAGE = stringPreferencesKey("source_language")
        private val TARGET_LANGUAGE = stringPreferencesKey("target_language")

        private val ALLOW_DATA_DOWNLOAD = booleanPreferencesKey("allow_data_download")
    }
}

private val Context.dataStore by preferencesDataStore("user_settings")
