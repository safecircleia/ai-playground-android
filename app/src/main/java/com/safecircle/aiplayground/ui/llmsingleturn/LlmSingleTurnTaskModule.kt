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

package com.safecircle.aiplayground.ui.llmsingleturn

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.runtime.Composable
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.customtasks.common.CustomTask
import com.safecircle.aiplayground.customtasks.common.CustomTaskDataForBuiltinTask
import com.safecircle.aiplayground.data.BuiltInTaskId
import com.safecircle.aiplayground.data.Category
import com.safecircle.aiplayground.data.Model
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.litertlm.Contents
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

class LlmSingleTurnTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_PROMPT_LAB,
      label = "Prompt Lab",
      category = Category.LLM,
      icon = Icons.Outlined.Widgets,
      models = mutableListOf(),
      description = "Test Horizon with a single prompt. Great for exploring the model's capabilities, experimenting with instructions, or running quick one-shot tasks — all processed on-device.",
      shortDescription = "One-shot prompts with Horizon",
      docUrl = "",
      sourceCodeUrl = "",
      textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
      defaultSystemPrompt = "You are Horizon, SafeCircle's helpful and safe on-device AI assistant. You are running entirely on the user's device — no data is sent to any server. Be concise, helpful, and friendly.",
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    systemInstruction: Contents?,
    onDone: (String) -> Unit,
  ) {
    LlmChatModelHelper.initialize(
      context = context,
      model = model,
      taskId = task.id,
      supportImage = false,
      supportAudio = false,
      onDone = onDone,
      systemInstruction = systemInstruction,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    LlmSingleTurnScreen(
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
    )
  }
}

@Module
@InstallIn(SingletonComponent::class) // Or another component that fits your scope
internal object LlmSingleTurnTaskModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return LlmSingleTurnTask()
  }
}
