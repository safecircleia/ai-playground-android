package com.safecircle.aiplayground.ui.common.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val STAGGER_DELAY_MS = 60L
private const val ENTRANCE_OFFSET_DP = 24

/**
 * Fades and slides an item up on first composition, delayed by [index] so a list cascades in.
 * When [enabled] is false the item is shown immediately.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int, enabled: Boolean): Modifier {
  var shown by remember { mutableStateOf(!enabled) }
  LaunchedEffect(enabled) {
    if (enabled) {
      delay(index * STAGGER_DELAY_MS)
      shown = true
    }
  }
  val progress by
    animateFloatAsState(
      targetValue = if (shown) 1f else 0f,
      animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
      label = "staggered entrance",
    )
  return graphicsLayer {
    alpha = progress.coerceIn(0f, 1f)
    translationY = ENTRANCE_OFFSET_DP.dp.toPx() * (1f - progress)
  }
}
