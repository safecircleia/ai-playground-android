package com.safecircle.aiplayground.ui.common.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

private const val PRESSED_SCALE = 0.97f

/** A clickable card that springs down slightly while pressed (M3 Expressive spatial motion). */
@Composable
fun ExpressiveCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
  shape: Shape = MaterialTheme.shapes.extraLargeIncreased,
  content: @Composable ColumnScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (pressed) PRESSED_SCALE else 1f,
      animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
      label = "card press scale",
    )
  Card(
    onClick = onClick,
    modifier =
      modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
      },
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    interactionSource = interactionSource,
    content = content,
  )
}
