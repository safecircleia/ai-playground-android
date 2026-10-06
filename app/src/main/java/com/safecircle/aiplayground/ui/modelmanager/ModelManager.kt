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

package com.safecircle.aiplayground.ui.modelmanager

// import androidx.compose.ui.tooling.preview.Preview
// import com.safecircle.aiplayground.ui.preview.PreviewModelManagerViewModel
// import com.safecircle.aiplayground.ui.preview.TASK_TEST1
// import com.safecircle.aiplayground.ui.theme.GalleryTheme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.GalleryTopAppBar
import com.safecircle.aiplayground.data.AppBarAction
import com.safecircle.aiplayground.data.AppBarActionType
import com.safecircle.aiplayground.data.Model
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.ui.common.TaskIcon
import com.safecircle.aiplayground.ui.common.expressive.sharedBadge

/** A screen to manage models. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManager(
  task: Task,
  viewModel: ModelManagerViewModel,
  enableAnimation: Boolean,
  navigateUp: () -> Unit,
  onModelClicked: (Model) -> Unit,
  modifier: Modifier = Modifier,
  onBenchmarkClicked: (Model) -> Unit = {},
  // True when shown as the detail pane beside the home list: no back button/handler, no shared
  // badge (the home card already shows it).
  embedded: Boolean = false,
) {
  // Set title based on the task.
  val title = task.label
  // Model count.
  val modelCount by remember {
    derivedStateOf {
      val trigger = task.updateTrigger.value
      if (trigger >= 0) {
        task.models.size
      } else {
        -1
      }
    }
  }

  // Navigate up when there are no models left.
  LaunchedEffect(modelCount) {
    if (modelCount == 0) {
      navigateUp()
    }
  }

  // Handle system's edge swipe.
  BackHandler(enabled = !embedded) { navigateUp() }

  Scaffold(
    modifier = modifier,
    topBar = {
      GalleryTopAppBar(
        title = title,
        leftAction =
          if (embedded) null
          else AppBarAction(actionType = AppBarActionType.NAVIGATE_UP, actionFn = navigateUp),
        titleIcon = {
          TaskIcon(
            task = task,
            modifier = Modifier.sharedBadge(task.id, enabled = !embedded),
            width = 32.dp,
          )
        },
      )
    },
  ) { innerPadding ->
    ModelList(
      task = task,
      modelManagerViewModel = viewModel,
      contentPadding = innerPadding,
      enableAnimation = enableAnimation,
      onModelClicked = onModelClicked,
      onBenchmarkClicked = onBenchmarkClicked,
      modifier = Modifier.fillMaxSize(),
    )
  }
}
