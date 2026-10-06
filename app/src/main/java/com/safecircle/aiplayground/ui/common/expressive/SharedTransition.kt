package com.safecircle.aiplayground.ui.common.expressive

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Marks a task icon as a shared element between the home card and the model list header. A no-op
 * outside a shared-transition layout, or when [enabled] is false (e.g. two panes on screen at once,
 * where the same key would appear twice).
 */
@Composable
fun Modifier.sharedBadge(taskId: String, enabled: Boolean = true): Modifier {
  val sharedScope = LocalSharedTransitionScope.current
  val visibilityScope = LocalAnimatedVisibilityScope.current
  if (!enabled || sharedScope == null || visibilityScope == null) return this
  return with(sharedScope) {
    this@sharedBadge.sharedElement(
      sharedContentState = rememberSharedContentState(key = "task-badge-$taskId"),
      animatedVisibilityScope = visibilityScope,
    )
  }
}
