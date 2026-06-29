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
import com.safecircle.aiplayground.ui.llmchat.LlmModelInstance
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.SessionConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SafetyDetectionVM"

// ponytail: trimmed to ~50 tokens so the 256-token model limit leaves room for conversation text
private const val HORIZON_SYSTEM_PROMPT =
    "Child safety risk analyzer. Reply ONLY with JSON: " +
    "{\"risk_level\":\"none|low|medium|high|critical\"," +
    "\"categories\":[\"grooming\",\"bullying\",\"sexual_content\",\"isolation\"," +
    "\"personal_info\",\"platform_migration\",\"threats\",\"benign\"]," +
    "\"confidence\":0.0,\"reasoning\":\"\"}"

// ~30 tokens of template overhead + ~50 prompt = ~80 reserved; leave ~170 tokens (~680 chars) for conversation
private const val MAX_CONVERSATION_CHARS = 680

// Keyword floor: if the 1B model misses obvious signals, elevate the result.
// Each entry: category → list of regex patterns (matched case-insensitively against message text).
private val KEYWORD_RULES: List<Pair<String, List<Regex>>> = listOf(
  "sexual_content" to listOf(
    // "dick pic/pick/pik/pix" — covers misspellings
    Regex("""dick\s*pi[ckx]""", RegexOption.IGNORE_CASE),
    Regex("""send\s*(me\s+)?(a\s+)?nud[ei]""", RegexOption.IGNORE_CASE),
    Regex("""nude\s*(pic|photo|image|selfie)""", RegexOption.IGNORE_CASE),
    Regex("""naked\s*(pic|photo|image|selfie)""", RegexOption.IGNORE_CASE),
    Regex("""show\s+me\s+(your\s+)?(body|boobs|tits|ass|cock|dick|pussy|vagina)""", RegexOption.IGNORE_CASE),
    Regex("""send\s+(a\s+)?(photo|pic|picture|image|selfie)\s+of\s+(your\s+)?(body|boobs|tits|ass|cock|dick|pussy|vagina)""", RegexOption.IGNORE_CASE),
    Regex("""sexual(ly)?\s*explicit""", RegexOption.IGNORE_CASE),
  ),
  "grooming" to listOf(
    Regex("""don.t tell (your )?(parents|mom|dad|family|anyone)""", RegexOption.IGNORE_CASE),
    Regex("""keep\s+(this|it|our\s+(chat|convo|conversation))\s+secret""", RegexOption.IGNORE_CASE),
    Regex("""our\s+little\s+secret""", RegexOption.IGNORE_CASE),
    Regex("""meet\s+(me\s+)?(up\s+)?(alone|in person|irl)""", RegexOption.IGNORE_CASE),
    Regex("""you.re\s+(so\s+)?(mature|special|different)\s+for\s+your\s+age""", RegexOption.IGNORE_CASE),
  ),
  "threats" to listOf(
    Regex("""i.ll\s+(hurt|kill|find|destroy|ruin)\s+you""", RegexOption.IGNORE_CASE),
    Regex("""i\s+will\s+(find|hurt|kill)\s+you""", RegexOption.IGNORE_CASE),
    Regex("""i\s+know\s+where\s+you\s+live""", RegexOption.IGNORE_CASE),
  ),
).map { (cat, patterns) -> cat to patterns }

// Risk levels match the model's training output schema.
enum class RiskLevel(val label: String, val modelValue: String) {
  NONE("Safe", "none"),
  LOW("Low Risk", "low"),
  MEDIUM("Medium Risk", "medium"),
  HIGH("High Risk", "high"),
  CRITICAL("Critical", "critical"),
}

val CATEGORY_LABELS = mapOf(
  "grooming" to "Grooming",
  "bullying" to "Bullying",
  "sexual_content" to "Sexual Content",
  "isolation" to "Isolation",
  "personal_info" to "Personal Info",
  "platform_migration" to "Platform Migration",
  "threats" to "Threats",
  "benign" to "Benign",
)

data class SafetyResult(
  val riskLevel: RiskLevel,
  val categories: List<String>,
  val confidence: Float,
  val reasoning: String,
  val rawResponse: String,   // full model output including any echo preamble
  val extractedJson: String, // the JSON block we actually parsed (empty = no JSON found)
  val parseFailed: Boolean = false,
)

enum class FeedbackSentiment { POSITIVE, NEGATIVE }

data class SafetyDetectionUiState(
  val isLoading: Boolean = false,
  val result: SafetyResult? = null,
  val errorMessage: String? = null,
  val feedbackSubmitted: Boolean = false,
  val lastMessages: List<ConversationMessage> = emptyList(),
  val lastModelName: String = "",
  val lastModelVersion: String = "",
)

@HiltViewModel
class SafetyDetectionViewModel @Inject constructor() : ViewModel() {

  private val _uiState = MutableStateFlow(SafetyDetectionUiState())
  val uiState: StateFlow<SafetyDetectionUiState> = _uiState.asStateFlow()

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  fun submitFeedback(sentiment: FeedbackSentiment, comment: String) {
    val state = _uiState.value
    val result = state.result ?: return

    logEvent(GalleryEvent.SAFETY_FEEDBACK, mapOf(
      "sentiment" to sentiment.name.lowercase(),
      "risk_level" to result.riskLevel.modelValue,
    ))

    _uiState.value = state.copy(feedbackSubmitted = true)

    viewModelScope.launch {
      runCatching {
        val payload = org.json.JSONObject().apply {
          put("sentiment", sentiment.name.lowercase())
          put("comment", comment)
          put("chat_type", "safety_detection")
          put("model_id", state.lastModelName)
          put("model_version", state.lastModelVersion)
          put("conversation", org.json.JSONArray().apply {
            state.lastMessages.forEach { msg ->
              put(org.json.JSONObject().apply {
                put("sender", msg.sender.name)
                put("text", msg.text)
              })
            }
          })
          put("model_response", result.rawResponse)
          put("parsed_risk_level", result.riskLevel.modelValue)
          put("parsed_categories", org.json.JSONArray(result.categories))
          put("parsed_confidence", result.confidence.toDouble())
        }

        withContext(Dispatchers.IO) {
          val url = java.net.URL("https://api.safecircle.tech/api/model-feedback")
          val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            outputStream.write(payload.toString().toByteArray())
          }
          Log.d(TAG, "Feedback POST: ${conn.responseCode}")
          conn.disconnect()
        }
      }.onFailure { e ->
        Log.w(TAG, "Failed to submit feedback", e)
      }
    }
  }

  fun analyzeMessages(modelManagerViewModel: ModelManagerViewModel, messages: List<ConversationMessage>) {
    val model = modelManagerViewModel.uiState.value.selectedModel ?: run {
      _uiState.value = _uiState.value.copy(errorMessage = "No model selected. Please download a Horizon model first.")
      return
    }
    val downloadStatus = modelManagerViewModel.uiState.value.modelDownloadStatus[model.name]?.status
    if (downloadStatus != ModelDownloadStatusType.SUCCEEDED) {
      _uiState.value = _uiState.value.copy(errorMessage = "Model not ready. Please download a Horizon model first.")
      return
    }

    _uiState.value = SafetyDetectionUiState(isLoading = true)

    viewModelScope.launch {
      runCatching {
        val instance = model.instance as? LlmModelInstance
          ?: throw IllegalStateException("Model not initialized")

        val conversationText = messages.joinToString("\n") { msg ->
          val role = if (msg.sender == MessageSender.OTHER) "Other" else "Child"
          "$role: ${msg.text}"
        }.take(MAX_CONVERSATION_CHARS)

        // Use Session API with raw formatted prompt (equivalent to --no-template).
        // The container template is unreliable, but the model works perfectly when
        // given the exact training format directly.
        val prompt = "<start_of_turn>user\n" +
          "$HORIZON_SYSTEM_PROMPT\n\n" +
          "Analyze this conversation:\n$conversationText<end_of_turn>\n" +
          "<start_of_turn>model\n"
        Log.d(TAG, "Sending raw prompt (${prompt.length} chars)")

        val fullResponse = withContext(Dispatchers.IO) {
          val session = instance.engine.createSession(
            SessionConfig(samplerConfig = SamplerConfig(40, 0.95, 0.3, 0))
          )
          try {
            session.generateContent(listOf(InputData.Text(prompt)))
          } finally {
            session.close()
          }
        }

        Log.d(TAG, "Raw response: $fullResponse")
        applyKeywordFloor(parseResponse(fullResponse), messages)
      }.onSuccess { result ->
        logEvent(GalleryEvent.SAFETY_SCAN, mapOf("risk_level" to result.riskLevel.name))
        _uiState.value = SafetyDetectionUiState(
          result = result,
          lastMessages = messages,
          lastModelName = model.name,
          lastModelVersion = model.version,
        )
      }.onFailure { e ->
        Log.e(TAG, "Safety analysis failed", e)
        _uiState.value = SafetyDetectionUiState(
          errorMessage = when {
            e.message?.contains("not initialized") == true ->
              "Model is still loading. Please wait a moment and try again."
            else -> e.message ?: "Analysis failed"
          }
        )
      }
    }
  }

  private fun applyKeywordFloor(result: SafetyResult, messages: List<ConversationMessage>): SafetyResult {
    val combinedText = messages.joinToString("\n") { it.text }
    val triggeredCategories = KEYWORD_RULES
      .filter { (_, patterns) -> patterns.any { it.containsMatchIn(combinedText) } }
      .map { (cat, _) -> cat }
    if (triggeredCategories.isEmpty()) return result

    Log.d(TAG, "Keyword floor triggered: $triggeredCategories (model said ${result.riskLevel})")

    // Only elevate, never downgrade.
    val floorLevel = RiskLevel.HIGH
    val elevatedLevel = if (result.riskLevel.ordinal >= floorLevel.ordinal) result.riskLevel else floorLevel
    val mergedCategories = (result.categories + triggeredCategories).distinct()
    return result.copy(
      riskLevel = elevatedLevel,
      categories = mergedCategories,
      confidence = result.confidence.coerceAtLeast(0.90f),
      reasoning = result.reasoning.ifBlank { "Detected explicit content patterns." },
    )
  }

  private fun parseResponse(raw: String): SafetyResult {
    // Strip markdown fences if present.
    val json = raw.trim()
      .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    // Extract the first {...} block. If none found, fall back to full text for field matching.
    val extractedJson = Regex("\\{[^{}]*\\}", RegexOption.DOT_MATCHES_ALL).find(json)?.value
    val jsonBlock = extractedJson ?: json

    // If no JSON object was found at all, this is a parse failure.
    if (extractedJson == null) {
      Log.w(TAG, "No JSON found in response: $raw")
      return SafetyResult(
        riskLevel = RiskLevel.NONE,
        categories = emptyList(),
        confidence = 0f,
        reasoning = "",
        rawResponse = raw,
        extractedJson = "",
        parseFailed = true,
      )
    }

    return runCatching {
      val riskLevelStr = Regex(""""risk_level"\s*:\s*"([^"]+)"""").find(jsonBlock)
        ?.groupValues?.get(1)?.lowercase()
        ?: Regex(""""severity"\s*:\s*"([^"]+)"""").find(jsonBlock)?.groupValues?.get(1)?.lowercase()
        ?: "none"

      val riskLevel = RiskLevel.entries.firstOrNull { it.modelValue == riskLevelStr }
        ?: RiskLevel.NONE

      val categoriesRaw = Regex(""""categories"\s*:\s*\[([^\]]*)]""").find(jsonBlock)
        ?.groupValues?.get(1) ?: ""
      val categories = Regex(""""([^"]+)"""").findAll(categoriesRaw)
        .map { it.groupValues[1] }
        .filter { it != "benign" }
        .toList()

      val singleCategory = Regex(""""category"\s*:\s*"([^"]+)"""").find(jsonBlock)
        ?.groupValues?.get(1)?.takeIf { it != "benign" }
      val allCategories = (categories + listOfNotNull(singleCategory)).distinct()

      val confidence = Regex(""""confidence"\s*:\s*([\d.]+)""").find(jsonBlock)
        ?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

      val reasoning = Regex(""""reasoning"\s*:\s*"([^"]+)"""").find(jsonBlock)
        ?.groupValues?.get(1) ?: ""

      Log.d(TAG, "Parsed: riskLevel=$riskLevel categories=$allCategories confidence=$confidence")

      SafetyResult(
        riskLevel = riskLevel,
        categories = allCategories,
        confidence = confidence,
        reasoning = reasoning,
        rawResponse = raw,
        extractedJson = extractedJson,
      )
    }.getOrElse { e ->
      Log.w(TAG, "Failed to parse JSON fields: $raw", e)
      SafetyResult(
        riskLevel = RiskLevel.NONE,
        categories = emptyList(),
        confidence = 0f,
        reasoning = "",
        rawResponse = raw,
        extractedJson = extractedJson,
        parseFailed = true,
      )
    }
  }

}
