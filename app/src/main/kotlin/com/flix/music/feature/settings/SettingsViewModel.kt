package com.flix.music.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.music.core.data.repository.SettingsRepository
import com.flix.music.core.model.AppTheme
import com.flix.music.core.model.UserSettings
import com.flix.music.core.system.WindowsAutostart
import com.flix.music.core.ui.WhileUiSubscribed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ajustes on the desktop: the phone's appearance and playback settings, plus "Iniciar
 * Flix con Windows", which lives in the registry rather than in DataStore.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val autostart: WindowsAutostart,
    val version: String,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, WhileUiSubscribed, UserSettings())

    val canStartWithWindows: Boolean = autostart.isAvailable

    var startsWithWindows by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch { startsWithWindows = withContext(Dispatchers.IO) { autostart.isEnabled() } }
    }

    fun onThemeChange(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun onAutoplayChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoplay(enabled) }
    }

    fun onRememberPlaybackModesChange(remember: Boolean) {
        viewModelScope.launch { settingsRepository.setRememberPlaybackModes(remember) }
    }

    fun onStartWithWindowsChange(enabled: Boolean) {
        startsWithWindows = enabled
        viewModelScope.launch {
            withContext(Dispatchers.IO) { autostart.setEnabled(enabled) }
            startsWithWindows = withContext(Dispatchers.IO) { autostart.isEnabled() }
        }
    }
}
