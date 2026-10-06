package com.safecircle.aiplayground.ui.common.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Square-ish expressive shapes; tasks cycle through them by index.
private val BadgePolygons =
  listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.SoftBurst,
    MaterialShapes.Flower,
  )

/** An icon container clipped to one of the Material expressive shapes. */
@Composable
fun ShapeBadge(
  index: Int,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
  content: @Composable () -> Unit,
) {
  val shape = BadgePolygons[index.coerceAtLeast(0) % BadgePolygons.size].toShape()
  Box(
    modifier = modifier.size(size).clip(shape).background(containerColor),
    contentAlignment = Alignment.Center,
  ) {
    CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
  }
}
