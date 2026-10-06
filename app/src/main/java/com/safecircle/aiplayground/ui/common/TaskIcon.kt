/*
 * Copyright 2026 SafeCircle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.safecircle.aiplayground.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.ui.common.expressive.ShapeBadge

/**
 * Icon representing a task: a glyph on an expressive-shape badge coloured from the theme.
 * [animationProgress] (0..1) scales/rotates/fades the badge in.
 */
@Composable
fun TaskIcon(
  task: Task,
  modifier: Modifier = Modifier,
  width: Dp = 56.dp,
  animationProgress: Float = 1f,
) {
  ShapeBadge(
    index = task.index,
    containerColor = getTaskBgColor(task),
    contentColor = getTaskOnBgColor(task),
    size = width,
    modifier =
      modifier.graphicsLayer {
        alpha = animationProgress
        scaleX = 0.6f + 0.4f * animationProgress
        scaleY = 0.6f + 0.4f * animationProgress
        rotationZ = -90f * (1f - animationProgress)
      },
  ) {
    Icon(
      task.icon ?: ImageVector.vectorResource(task.iconVectorResourceId!!),
      contentDescription = null,
      modifier = Modifier.size(width * 0.5f),
    )
  }
}
