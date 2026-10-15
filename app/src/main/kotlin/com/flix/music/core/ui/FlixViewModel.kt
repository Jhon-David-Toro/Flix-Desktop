package com.flix.music.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flix.music.AppGraph

/** The app's object graph, provided once by the window. */
val LocalAppGraph = staticCompositionLocalOf<AppGraph> { error("AppGraph not provided: wrap the UI in the app window") }

/**
 * The desktop's `hiltViewModel()`: a ViewModel scoped to the current screen, built by
 * [AppGraph] with the screen's navigation arguments.
 */
@Composable
inline fun <reified Model : ViewModel> flixViewModel(): Model {
    val graph = LocalAppGraph.current
    return viewModel { graph.viewModel(Model::class, createSavedStateHandle()) }
}
