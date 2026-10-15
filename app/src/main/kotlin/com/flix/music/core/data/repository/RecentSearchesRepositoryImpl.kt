package com.flix.music.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Stores the recent searches as a JSON list in Preferences DataStore.
 * An unreadable file falls back to an empty list instead of crashing.
 */
class RecentSearchesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : RecentSearchesRepository {

    override val recentSearches: Flow<List<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences.readRecents() }

    override suspend fun add(query: String) = update { recents -> recents.withRecent(query) }

    override suspend fun remove(query: String) =
        update { recents -> recents.filterNot { it.equals(query, ignoreCase = true) } }

    override suspend fun clear() = update { emptyList() }

    private suspend fun update(transform: (List<String>) -> List<String>) {
        dataStore.edit { preferences ->
            preferences[RECENTS_KEY] = json.encodeToString(transform(preferences.readRecents()))
        }
    }

    private fun Preferences.readRecents(): List<String> =
        this[RECENTS_KEY]?.let { stored -> json.decodeFromString<List<String>>(stored) } ?: emptyList()

    private companion object {
        val RECENTS_KEY = stringPreferencesKey("recent_searches")
    }
}
