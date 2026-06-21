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

package com.safecircle.aiplayground.ui.safety

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.safecircle.aiplayground.customtasks.common.CustomTask
import com.safecircle.aiplayground.customtasks.common.CustomTaskDataForBuiltinTask
import com.safecircle.aiplayground.data.BuiltInTaskId
import com.safecircle.aiplayground.data.Category
import com.safecircle.aiplayground.data.Model
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.runtime.runtimeHelper
import com.google.ai.edge.litertlm.Contents
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

class SafetyDetectionTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = BuiltInTaskId.SAFETY_DETECTION,
      label = "Safety Detection",
      category = Category.LLM,
      icon = Icons.Outlined.Shield,
      models = mutableListOf(),
      description = "Paste any text and see how Horizon classifies it in real time. Detects grooming, bullying, threats, and more — directly on your device in under 100ms. No data is ever sent to a server.",
      shortDescription = "Detect safety risks on-device",
      docUrl = "",
      sourceCodeUrl = "",
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    systemInstruction: Contents?,
    onDone: (String) -> Unit,
  ) {
    model.runtimeHelper.initialize(
      context = context,
      model = model,
      taskId = task.id,
      supportImage = false,
      supportAudio = false,
      onDone = onDone,
      coroutineScope = coroutineScope,
      systemInstruction = null,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    model.runtimeHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    val viewModel: SafetyDetectionViewModel = hiltViewModel()
    SafetyDetectionScreen(
      modelManagerViewModel = myData.modelManagerViewModel,
      viewModel = viewModel,
      navigateUp = myData.onNavUp,
    )
  }
}

@Module
@InstallIn(SingletonComponent::class)
internal object SafetyDetectionTaskModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return SafetyDetectionTask()
  }
}
