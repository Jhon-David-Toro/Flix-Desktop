package com.flix.music.core.designsystem.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/** Screen transitions from the design's Motion section. */
object FlixTransitions {

    /** Tab switch: fade-through, 90 ms out then 150 ms in. */
    fun tabEnter(): EnterTransition =
        fadeIn(tween(FlixMotion.TAB_ENTER, delayMillis = FlixMotion.TAB_EXIT, easing = FlixMotion.Decelerate))

    fun tabExit(): ExitTransition =
        fadeOut(tween(FlixMotion.TAB_EXIT, easing = FlixMotion.Accelerate))

    /** Push to a detail screen: enters 24 dp from the right with a fade (normal). */
    fun pushEnter(): EnterTransition =
        slideInHorizontally(tween(FlixMotion.NORMAL, easing = FlixMotion.Decelerate)) { fullWidth ->
            fullWidth / PUSH_OFFSET_DIVISOR
        } + fadeIn(tween(FlixMotion.NORMAL, easing = FlixMotion.Decelerate))

    /** Back from a detail screen: mirror of [pushEnter]. */
    fun pushExit(): ExitTransition =
        slideOutHorizontally(tween(FlixMotion.NORMAL, easing = FlixMotion.Accelerate)) { fullWidth ->
            fullWidth / PUSH_OFFSET_DIVISOR
        } + fadeOut(tween(FlixMotion.NORMAL, easing = FlixMotion.Accelerate))

    /** 1/15 of the width is 24 dp on the 360 dp reference phone. */
    private const val PUSH_OFFSET_DIVISOR = 15
}
