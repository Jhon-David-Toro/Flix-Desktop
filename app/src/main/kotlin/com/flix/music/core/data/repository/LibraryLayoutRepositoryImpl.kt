package com.flix.music.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class LibraryLayoutRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : LibraryLayoutRepository {

    override val gridFilters: Flow<Set<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[GRID_FILTERS_KEY].orEmpty() }

    override suspend fun setGrid(filterKey: String, grid: Boolean) {
        dataStore.edit { preferences ->
            val current = preferences[GRID_FILTERS_KEY].orEmpty()
            preferences[GRID_FILTERS_KEY] = if (grid) current + filterKey else current - filterKey
        }
    }

    private companion object {
        val GRID_FILTERS_KEY = stringSetPreferencesKey("library_grid_filters")
    }
}
