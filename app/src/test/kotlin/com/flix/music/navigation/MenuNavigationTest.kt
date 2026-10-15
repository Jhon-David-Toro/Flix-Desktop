package com.flix.music.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import org.junit.Assert.assertTrue
import org.junit.Test

@Serializable internal data object HomeTab

@Serializable internal data object SearchTab

@Serializable internal data object SearchScreenRoute

@Serializable internal data object LibraryTab

@Serializable internal data object LibraryScreenRoute

@Serializable internal data object SettingsMenu

/** The side menu's tabs and Ajustes, laid out like FlixNavHost: tabs are graphs, Ajustes stands alone. */
@OptIn(ExperimentalTestApi::class)
class MenuNavigationTest {

    @Test
    fun `volver a Buscar tras pasar por Ajustes muestra Buscar, no Ajustes`() = runComposeUiTest {
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = HomeTab) {
                composable<HomeTab> { Text("Inicio") }
                navigation<SearchTab>(startDestination = SearchScreenRoute) { composable<SearchScreenRoute> { Text("Buscar") } }
                navigation<LibraryTab>(startDestination = LibraryScreenRoute) { composable<LibraryScreenRoute> { Text("Biblioteca") } }
                composable<SettingsMenu> { Text("Ajustes") }
            }
        }

        runOnIdle { navController.navigateToMenuRoute(SearchTab) }
        runOnIdle { navController.navigateToMenuRoute(SettingsMenu) }
        runOnIdle { navController.navigateToMenuRoute(LibraryTab) }
        runOnIdle { navController.navigateToMenuRoute(SearchTab) }

        runOnIdle {
            val destination = requireNotNull(navController.currentDestination)
            assertTrue("debería estar en Buscar", destination.hasRoute(SearchScreenRoute::class))
        }
    }
}
