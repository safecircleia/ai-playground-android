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

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlin.math.exp

/** Max option markers per question; the exported graph takes `[2, MAX_MARKERS]` marker positions. */
private const val MAX_MARKERS = 8

/** The two question rows of one conversation, as the exported graph takes them: int32, `[2, ...]`. */
class VigilBatch(
  val inputIds: IntArray, // [2 * seqLen]
  val attentionMask: IntArray, // [2 * seqLen]
  val markerPos: IntArray, // [2 * MAX_MARKERS]
  val markerMask: IntArray, // [2 * MAX_MARKERS]
  val qtype: IntArray, // [2]
)

/** Runs the exported encoder graph: marker logits `[2][MAX_MARKERS]` for the category and severity rows. */
interface VigilGraph : AutoCloseable {
  fun run(batch: VigilBatch): Array<FloatArray>
}

data class VigilQuestionSpec(
  @SerializedName("prefix_ids") val prefixIds: IntArray,
  @SerializedName("marker_pos") val markerPos: IntArray,
  @SerializedName("qtype") val qtype: Int,
  @SerializedName("temperature") val temperature: Double,
)

/** `vigil_spec.json`, written by scripts/export_litert.py next to the model. */
data class VigilSpec(
  @SerializedName("seq_len") val seqLen: Int,
  @SerializedName("sep_id") val sepId: Int,
  @SerializedName("mask_token") val maskToken: String,
  @SerializedName("categories") val categories: List<String>,
  @SerializedName("severity_levels") val severityLevels: List<String>,
  @SerializedName("thresholds") val thresholds: Map<String, Double>,
  @SerializedName("questions") val questions: Map<String, VigilQuestionSpec>,
) {
  companion object {
    fun parse(json: String): VigilSpec = Gson().fromJson(json, VigilSpec::class.java)
  }
}

data class VigilPrediction(
  /** Probability per risk category, in [VigilSpec.categories] order (the "benign" option is excluded). */
  val categoryProbs: Map<String, Double>,
  /** Probability per severity level, in [VigilSpec.severityLevels] order. */
  val severityProbs: List<Double>,
  /** Probability that the conversation shows any risk: 1 - P(benign). */
  val riskProbability: Double,
  /** Categories at or above their calibrated threshold. */
  val flagged: List<String>,
  /** Severity level name; "none" unless a category is flagged, and never "none" when one is. */
  val riskLevel: String,
  val reasoning: String,
) {
  /** Expected severity, 0 (none) to 4 (critical). */
  val severityScore: Double
    get() = severityProbs.withIndex().sumOf { (i, p) -> i * p }
}

/** The prediction in Horizon's JSON schema, for callers that already parse that. */
fun VigilPrediction.toHorizonJson(): String {
  val categories = flagged.joinToString(",") { "\"$it\"" }
  val confidence = if (flagged.isEmpty()) 0.0 else riskProbability
  return "{\"risk_level\":\"$riskLevel\",\"categories\":[$categories]," +
    "\"confidence\":%.4f,\"reasoning\":\"$reasoning\"}".format(java.util.Locale.US, confidence)
}

/**
 * Vigil's inference around the exported graph. Mirrors `OnDevice` in vigil/ondevice.py: the question
 * prefixes are precomputed in the spec, so a conversation is tokenized once, placed after each prefix
 * (keeping its most recent tokens if it is too long), padded, and scored; the softmax uses the
 * checkpoint's temperature and a category is flagged at its own threshold.
 */
class VigilClassifier(
  private val spec: VigilSpec,
  private val tokenizer: VigilTokenizer,
  private val graph: VigilGraph,
) : AutoCloseable {
  private val categoryQuestion = spec.questions.getValue("category")
  private val severityQuestion = spec.questions.getValue("severity")

  fun classify(conversation: String): VigilPrediction {
    val logits = graph.run(encode(conversation))
    return decode(logits)
  }

  internal fun encode(conversation: String): VigilBatch {
    val s = spec.seqLen
    val stateIds = tokenizer.encode(conversation.replace(spec.maskToken, " "))
    val inputIds = IntArray(2 * s)
    val mask = IntArray(2 * s)
    val markerPos = IntArray(2 * MAX_MARKERS)
    val markerMask = IntArray(2 * MAX_MARKERS)
    val qtype = IntArray(2)
    listOf(categoryQuestion, severityQuestion).forEachIndexed { row, q ->
      val room = maxOf(0, s - q.prefixIds.size - 1)
      val state = stateIds.copyOfRange(maxOf(0, stateIds.size - room), stateIds.size)
      val ids = q.prefixIds + state + spec.sepId
      ids.copyInto(inputIds, row * s)
      mask.fill(1, row * s, row * s + ids.size)
      q.markerPos.copyInto(markerPos, row * MAX_MARKERS)
      markerMask.fill(1, row * MAX_MARKERS, row * MAX_MARKERS + q.markerPos.size)
      qtype[row] = q.qtype
    }
    return VigilBatch(inputIds, mask, markerPos, markerMask, qtype)
  }

  internal fun decode(logits: Array<FloatArray>): VigilPrediction {
    val category = softmax(logits[0], categoryQuestion.markerPos.size, categoryQuestion.temperature)
    val severity = softmax(logits[1], severityQuestion.markerPos.size, severityQuestion.temperature)
    // Option 0 of the category question is "benign"; the risk categories follow in spec order.
    val probs = spec.categories.withIndex().associate { (i, name) -> name to category[i + 1] }
    val flagged = spec.categories.filter { probs.getValue(it) >= spec.thresholds.getValue(it) }
    // Training ties severity "none" to "no category", so a flagged conversation takes its most likely level above none.
    val level =
      if (flagged.isEmpty()) spec.severityLevels[0]
      else spec.severityLevels[(1 until severity.size).maxByOrNull { severity[it] }!!]
    return VigilPrediction(
      categoryProbs = probs,
      severityProbs = severity.toList(),
      riskProbability = 1.0 - category[0],
      flagged = flagged,
      riskLevel = level,
      reasoning =
        flagged.joinToString("; ") {
          "$it p=%.3f (flag at %.3f)".format(java.util.Locale.US, probs.getValue(it), spec.thresholds.getValue(it))
        },
    )
  }

  override fun close() = graph.close()

  private fun softmax(logits: FloatArray, n: Int, temperature: Double): DoubleArray {
    val z = DoubleArray(n) { logits[it] / temperature }
    val max = z.max()
    val e = DoubleArray(n) { exp(z[it] - max) }
    val sum = e.sum()
    return DoubleArray(n) { e[it] / sum }
  }
}
