package com.flix.music.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flix.music.core.model.AppTheme
import com.flix.music.core.model.PlaybackModes
import com.flix.music.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val repeatMode = preferences[REPEAT_MODE_KEY]
            UserSettings(
                theme = preferences[THEME_KEY]?.let { name -> AppTheme.entries.firstOrNull { it.name == name } }
                    ?: AppTheme.System,
                autoplay = preferences[AUTOPLAY_KEY] ?: true,
                rememberPlaybackModes = preferences[REMEMBER_MODES_KEY] ?: false,
                playbackModes = repeatMode?.let { mode -> PlaybackModes(preferences[SHUFFLE_KEY] ?: false, mode) },
                onboardingCompleted = preferences[ONBOARDING_KEY] ?: false,
            )
        }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences -> preferences[THEME_KEY] = theme.name }
    }

    override suspend fun setAutoplay(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[AUTOPLAY_KEY] = enabled }
    }

    override suspend fun setRememberPlaybackModes(remember: Boolean) {
        dataStore.edit { preferences -> preferences[REMEMBER_MODES_KEY] = remember }
    }

    override suspend fun savePlaybackModes(modes: PlaybackModes) {
        dataStore.edit { preferences ->
            preferences[SHUFFLE_KEY] = modes.shuffle
            preferences[REPEAT_MODE_KEY] = modes.repeatMode
        }
    }

    override suspend fun completeOnboarding() {
        dataStore.edit { preferences -> preferences[ONBOARDING_KEY] = true }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val AUTOPLAY_KEY = booleanPreferencesKey("autoplay")
        val REMEMBER_MODES_KEY = booleanPreferencesKey("remember_playback_modes")
        val SHUFFLE_KEY = booleanPreferencesKey("shuffle")
        val REPEAT_MODE_KEY = stringPreferencesKey("repeat_mode")
        val ONBOARDING_KEY = booleanPreferencesKey("onboarding_completed")
    }
}
