package com.flix.music.core.designsystem.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object FlixMotion {
    const val FAST = 120
    const val NORMAL = 240
    const val SLOW = 400
    const val TAB_EXIT = 90
    const val TAB_ENTER = 150

    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Decelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
    val Accelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    fun <T> fast(): AnimationSpec<T> = tween(FAST, easing = Standard)
    fun <T> normal(): AnimationSpec<T> = tween(NORMAL, easing = Standard)
    fun <T> slow(): AnimationSpec<T> = tween(SLOW, easing = Standard)
    fun <T> gesture(): AnimationSpec<T> = spring(dampingRatio = 0.85f, stiffness = 500f)
}