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

package com.safecircle.aiplayground.runtime.vigil

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.ToolProvider
import com.safecircle.aiplayground.data.Model
import com.safecircle.aiplayground.runtime.CleanUpListener
import com.safecircle.aiplayground.runtime.LlmModelHelper
import com.safecircle.aiplayground.runtime.ResultListener
import java.io.File
import kotlinx.coroutines.CoroutineScope

private const val TAG = "VigilModelHelper"
private const val ASSET_DIR = "vigil"

data class VigilModelInstance(val classifier: VigilClassifier)

/**
 * Runtime for Vigil, SafeCircle's encoder classifier. It is not a text generator, so most of
 * [LlmModelHelper] does not apply: the Safety Detection screen calls [VigilClassifier] directly, and
 * [runInference] is only a convenience that answers in Horizon's JSON schema.
 *
 * The spec (thresholds, question prefixes) and the tokenizer ship in the app's assets, because they
 * belong to one trained checkpoint; the .tflite is the large downloaded or imported file.
 */
object VigilModelHelper : LlmModelHelper {
  override fun initialize(
    context: Context,
    model: Model,
    taskId: String,
    supportImage: Boolean,
    supportAudio: Boolean,
    onDone: (String) -> Unit,
    systemInstruction: Contents?,
    tools: List<ToolProvider>,
    enableConversationConstrainedDecoding: Boolean,
    coroutineScope: CoroutineScope?,
  ) {
    try {
      val spec = VigilSpec.parse(context.assets.open("$ASSET_DIR/vigil_spec.json").bufferedReader().use { it.readText() })
      val tokenizer = VigilTokenizer.parse(context.assets.open("$ASSET_DIR/vigil_tokenizer.bin").use { it.readBytes() })
      val graph = LiteRtVigilGraph(File(model.getPath(context)))
      model.instance = VigilModelInstance(VigilClassifier(spec, tokenizer, graph))
      onDone("")
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to initialize Vigil", e)
      onDone(e.message ?: "Failed to load the Vigil model")
    }
  }

  override fun resetConversation(
    model: Model,
    supportImage: Boolean,
    supportAudio: Boolean,
    systemInstruction: Contents?,
    tools: List<ToolProvider>,
    enableConversationConstrainedDecoding: Boolean,
    initialMessages: List<Message>,
  ) = Unit // stateless: every call scores the whole conversation

  override fun cleanUp(model: Model, onDone: () -> Unit) {
    val instance = model.instance as? VigilModelInstance
    if (instance != null) {
      try {
        instance.classifier.close()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to close the Vigil model: ${e.message}")
      }
    }
    model.instance = null
    onDone()
  }

  override fun runInference(
    model: Model,
    input: String,
    resultListener: ResultListener,
    cleanUpListener: CleanUpListener,
    onError: (message: String) -> Unit,
    images: List<Bitmap>,
    audioClips: List<ByteArray>,
    coroutineScope: CoroutineScope?,
    extraContext: Map<String, String>?,
  ) {
    val instance = model.instance as? VigilModelInstance
    if (instance == null) {
      onError("Model not initialized")
      return
    }
    try {
      resultListener(instance.classifier.classify(input).toHorizonJson(), true, null)
    } catch (e: Exception) {
      onError(e.message ?: "Vigil inference failed")
    } finally {
      cleanUpListener()
    }
  }

  override fun stopResponse(model: Model) = Unit // a single forward pass; nothing to cancel
}
