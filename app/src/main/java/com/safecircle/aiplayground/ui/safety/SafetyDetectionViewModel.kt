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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safecircle.aiplayground.GalleryEvent
import com.safecircle.aiplayground.logEvent
import com.safecircle.aiplayground.data.ModelDownloadStatusType
import com.safecircle.aiplayground.runtime.runtimeHelper
import com.safecircle.aiplayground.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.litertlm.Contents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "SafetyDetectionVM"

internal const val SAFETY_SYSTEM_PROMPT = """You are a content safety classifier. Analyze the given text and respond ONLY in this exact JSON format (no markdown, no extra text):
{"risk_level":"SAFE|LOW|MEDIUM|HIGH","categories":["category1","category2"],"explanation":"brief reason"}

Risk levels:
- SAFE: No harmful content detected
- LOW: Mildly inappropriate, not harmful
- MEDIUM: Moderately harmful, concerning
- HIGH: Severely harmful, dangerous

Categories (use only applicable ones): violence, self-harm, hate-speech, sexual, misinformation, harassment, spam, other

Respond with JSON only."""

enum class RiskLevel(val label: String) {
  SAFE("Safe"),
  LOW("Low Risk"),
  MEDIUM("Medium Risk"),
  HIGH("High Risk"),
}

data class SafetyResult(
  val riskLevel: RiskLevel,
  val categories: List<String>,
  val explanation: String,
)

data class SafetyDetectionUiState(
  val isLoading: Boolean = false,
  val result: SafetyResult? = null,
  val errorMessage: String? = null,
)

@HiltViewModel
class SafetyDetectionViewModel @Inject constructor() : ViewModel() {

  private val _uiState = MutableStateFlow(SafetyDetectionUiState())
  val uiState: StateFlow<SafetyDetectionUiState> = _uiState.asStateFlow()

  fun analyze(modelManagerViewModel: ModelManagerViewModel, text: String) {
    val model = modelManagerViewModel.uiState.value.selectedModel ?: run {
      _uiState.value = _uiState.value.copy(errorMessage = "No model selected. Please download a model first.")
      return
    }
    val downloadStatus = modelManagerViewModel.uiState.value.modelDownloadStatus[model.name]?.status
    if (downloadStatus != ModelDownloadStatusType.SUCCEEDED) {
      _uiState.value = _uiState.value.copy(errorMessage = "Model not ready. Please download a model first.")
      return
    }

    _uiState.value = SafetyDetectionUiState(isLoading = true)

    viewModelScope.launch {
      runCatching {
        // Reset conversation with safety system prompt to avoid cross-task context bleed.
        model.runtimeHelper.resetConversation(
          model = model,
          systemInstruction = Contents.of(SAFETY_SYSTEM_PROMPT),
        )

        // Accumulate the streaming response.
        val fullResponse = suspendCancellableCoroutine { cont ->
          val sb = StringBuilder()
          model.runtimeHelper.runInference(
            model = model,
            input = text,
            resultListener = { partial, done, _ ->
              if (!partial.startsWith("<ctrl")) sb.append(partial)
              if (done) cont.resume(sb.toString())
            },
            cleanUpListener = {},
            onError = { msg -> cont.resumeWithException(RuntimeException(msg)) },
            coroutineScope = viewModelScope,
          )
        }

        parseResponse(fullResponse)
      }.onSuccess { result ->
        logEvent(GalleryEvent.SAFETY_SCAN, mapOf("risk_level" to result.riskLevel.name))
        _uiState.value = SafetyDetectionUiState(result = result)
      }.onFailure { e ->
        Log.e(TAG, "Safety analysis failed", e)
        _uiState.value = SafetyDetectionUiState(errorMessage = e.message ?: "Analysis failed")
      }
    }
  }

  private fun parseResponse(raw: String): SafetyResult {
    val json = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    return runCatching {
      val riskLevelStr = Regex(""""risk_level"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1) ?: "SAFE"
      val riskLevel = RiskLevel.entries.firstOrNull { it.name == riskLevelStr } ?: RiskLevel.SAFE
      val categoriesRaw = Regex(""""categories"\s*:\s*\[([^\]]*)]""").find(json)?.groupValues?.get(1) ?: ""
      val categories = Regex(""""([^"]+)"""").findAll(categoriesRaw).map { it.groupValues[1] }.toList()
      val explanation = Regex(""""explanation"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1) ?: ""
      SafetyResult(riskLevel = riskLevel, categories = categories, explanation = explanation)
    }.getOrElse {
      SafetyResult(riskLevel = RiskLevel.SAFE, categories = emptyList(), explanation = raw.take(200))
    }
  }
}
