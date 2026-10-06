package com.safecircle.aiplayground.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset

/** Navigation transitions driven by the theme's [androidx.compose.material3.MotionScheme]. */
internal class NavTransitions(
  private val offsetSpec: FiniteAnimationSpec<IntOffset>,
  private val fadeSpec: FiniteAnimationSpec<Float>,
) {
  fun slideEnter(scope: AnimatedContentTransitionScope<*>): EnterTransition =
    scope.slideIntoContainer(
      towards = AnimatedContentTransitionScope.SlideDirection.Left,
      animationSpec = offsetSpec,
    ) + fadeIn(fadeSpec)

  fun slideExit(scope: AnimatedContentTransitionScope<*>): ExitTransition =
    scope.slideOutOfContainer(
      towards = AnimatedContentTransitionScope.SlideDirection.Right,
      animationSpec = offsetSpec,
    ) + fadeOut(fadeSpec)

  fun slideUpEnter(scope: AnimatedContentTransitionScope<*>): EnterTransition =
    scope.slideIntoContainer(
      towards = AnimatedContentTransitionScope.SlideDirection.Up,
      animationSpec = offsetSpec,
    ) + fadeIn(fadeSpec)

  fun slideDownExit(scope: AnimatedContentTransitionScope<*>): ExitTransition =
    scope.slideOutOfContainer(
      towards = AnimatedContentTransitionScope.SlideDirection.Down,
      animationSpec = offsetSpec,
    ) + fadeOut(fadeSpec)
}

@Composable
internal fun rememberNavTransitions(): NavTransitions {
  val motion = MaterialTheme.motionScheme
  return remember(motion) { NavTransitions(motion.defaultSpatialSpec(), motion.defaultEffectsSpec()) }
}
