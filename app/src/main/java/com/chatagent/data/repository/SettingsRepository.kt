package com.chatagent.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chatagent.data.model.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val encryptedPreferences: SharedPreferences
) {
    companion object {
        private val KEY_PROVIDER = stringPreferencesKey("provider")
        private val KEY_MODEL = stringPreferencesKey("model")
        private val KEY_CUSTOM_URL = stringPreferencesKey("custom_url")
        private val KEY_THEME = stringPreferencesKey("theme")
        private fun keyApiKey(provider: String) = "api_key_$provider"
    }

    val currentProvider: Flow<ApiProvider> = dataStore.data.map { prefs ->
        ApiProvider.fromName(prefs[KEY_PROVIDER] ?: "AGNES")
    }

    val currentModel: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_MODEL] ?: ""
    }

    val customUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_URL] ?: ""
    }

    val isDarkTheme: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_THEME] != "light"
    }

    fun getApiKey(provider: ApiProvider): Flow<String> {
        val key = keyApiKey(provider.name)
        return kotlinx.coroutines.flow.flow {
            emit(encryptedPreferences.getString(key, "") ?: "")
        }
    }

    suspend fun setProvider(provider: ApiProvider) {
        dataStore.edit { prefs ->
            prefs[KEY_PROVIDER] = provider.name
        }
    }

    suspend fun setModel(model: String) {
        dataStore.edit { prefs ->
            prefs[KEY_MODEL] = model
        }
    }

    suspend fun setCustomUrl(url: String) {
        dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_URL] = url
        }
    }

    suspend fun setApiKey(provider: ApiProvider, apiKey: String) {
        withContext(Dispatchers.IO) {
            encryptedPreferences.edit {
                putString(keyApiKey(provider.name), apiKey)
            }
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME] = if (isDark) "dark" else "light"
        }
    }
}
