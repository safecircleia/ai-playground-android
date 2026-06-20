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

package com.safecircle.aiplayground

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

private const val TAG = "SCAnalytics"
private const val TELEMETRY_URL = "https://backend.safecircle.tech/api/telemetry/event"

private val analyticsClient = HttpClient(Android) {
  expectSuccess = false
}

private val analyticsScope = CoroutineScope(Dispatchers.IO)

@Serializable
private data class TelemetryEvent(
  val name: String,
  val route: String? = null,
  val props: Map<String, JsonElement>? = null,
)

fun logEvent(event: GalleryEvent, props: Map<String, String> = emptyMap()) {
  analyticsScope.launch {
    runCatching {
      val body = TelemetryEvent(
        name = event.id,
        route = "android_playground",
        props = props.ifEmpty { null }?.mapValues { Json.encodeToJsonElement(it.value) },
      )
      analyticsClient.post(TELEMETRY_URL) {
        setBody(TextContent(Json.encodeToString(TelemetryEvent.serializer(), body), ContentType.Application.Json))
      }
    }.onFailure { Log.w(TAG, "Failed to send analytics event: ${event.id}", it) }
  }
}

enum class GalleryEvent(val id: String) {
  CAPABILITY_SELECT(id = "playground_capability_select"),
  MODEL_DOWNLOAD(id = "playground_model_download"),
  GENERATE_ACTION(id = "playground_generate_action"),
  BUTTON_CLICKED(id = "playground_button_clicked"),
  SAFETY_SCAN(id = "playground_safety_scan"),
  CHAT_MESSAGE(id = "playground_chat_message"),
  BENCHMARK_RUN(id = "playground_benchmark_run"),
  CHAT_HISTORY(id = "playground_chat_history"),
}
