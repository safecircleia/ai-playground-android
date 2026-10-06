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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.ui.theme.customColors

@Composable
fun getTaskBgColor(task: Task): Color {
  val colorIndex: Int = (task.index.coerceAtLeast(0)) % MaterialTheme.customColors.taskBgColors.size
  return MaterialTheme.customColors.taskBgColors[colorIndex]
}

@Composable
fun getTaskBgGradientColors(task: Task): List<Color> {
  val colorIndex: Int = (task.index.coerceAtLeast(0)) % MaterialTheme.customColors.taskBgColors.size
  return MaterialTheme.customColors.taskBgGradientColors[colorIndex]
}

@Composable
fun getTaskIconColor(task: Task): Color {
  val colorIndex: Int =
    (task.index.coerceAtLeast(0)) % MaterialTheme.customColors.taskIconColors.size
  return MaterialTheme.customColors.taskIconColors[colorIndex]
}

/** Text/icon colour for content drawn on [getTaskBgColor]. */
@Composable
fun getTaskOnBgColor(task: Task): Color {
  val colors = MaterialTheme.customColors.taskOnBgColors
  return colors[task.index.coerceAtLeast(0) % colors.size]
}

@Composable
fun getTaskOnIconColor(task: Task): Color {
  val colors = MaterialTheme.customColors.taskOnIconColors
  return colors[task.index.coerceAtLeast(0) % colors.size]
}

@Composable
fun getTaskIconColor(index: Int): Color {
  val colorIndex: Int = (index.coerceAtLeast(0)) % MaterialTheme.customColors.taskIconColors.size
  return MaterialTheme.customColors.taskIconColors[colorIndex]
}
