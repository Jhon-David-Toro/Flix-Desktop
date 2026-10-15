package com.flix.music.core.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

/**
 * Opens [route] only from a screen that is fully shown. A second tap while the first
 * push is still animating is dropped, so the same screen is never stacked twice and
 * one Back always leaves it.
 */
fun NavController.navigateFromResumed(route: Any) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) navigate(route)
}
