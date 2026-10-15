package com.flix.music.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.flix.music.core.designsystem.theme.FlixTransitions
import com.flix.music.core.navigation.navigateFromResumed
import com.flix.music.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavController.navigateToSettings() = navigateFromResumed(SettingsRoute)

/** Ajustes on the desktop: reached from the side menu or Perfil, with no back arrow of its own. */
fun NavGraphBuilder.settingsScreen() {
    composable<SettingsRoute>(
        enterTransition = { FlixTransitions.pushEnter() },
        popExitTransition = { FlixTransitions.pushExit() },
    ) {
        SettingsScreen()
    }
}
