package com.chatagent.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chatagent.data.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ConversationStorage @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_CONVERSATIONS = stringPreferencesKey("conversations")
        private val json = Json { ignoreUnknownKeys = true }
    }

    val conversations: Flow<List<Conversation>> = dataStore.data.map { prefs ->
        val data = prefs[KEY_CONVERSATIONS] ?: "[]"
        try {
            json.decodeFromString<List<Conversation>>(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveConversations(conversations: List<Conversation>) {
        dataStore.edit { prefs ->
            prefs[KEY_CONVERSATIONS] = json.encodeToString(conversations)
        }
    }
}
