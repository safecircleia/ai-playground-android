package com.safecircle.aiplayground.runtime.vigil

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private data class TokenizerCase(val text: String, val ids: IntArray)

private data class InferenceCase(
  val text: String,
  @SerializedName("category_ids") val categoryIds: IntArray,
  @SerializedName("severity_ids") val severityIds: IntArray,
)

// Gradle runs unit tests with the module directory as the working directory.
private val ASSETS = File("src/main/assets/vigil")

private fun <T> golden(name: String, type: Class<Array<T>>): Array<T> =
  Gson().fromJson(
    VigilTokenizerTest::class.java.getResourceAsStream("/vigil/$name")!!.bufferedReader().readText(),
    type,
  )

private val tokenizer = VigilTokenizer.parse(File(ASSETS, "vigil_tokenizer.bin").readBytes())

class VigilTokenizerTest {
  @Test
  fun matchesHuggingFaceOnGoldenStrings() {
    val cases = golden("tokenizer_golden.json", Array<TokenizerCase>::class.java)
    assertTrue(cases.size > 100)
    for (case in cases) {
      assertArrayEquals("text: ${case.text.take(60)}", case.ids, tokenizer.encode(case.text))
    }
  }
}

class VigilClassifierTest {
  private val spec = VigilSpec.parse(File(ASSETS, "vigil_spec.json").readText())
  private val noGraph =
    object : VigilGraph {
      override fun run(batch: VigilBatch): Array<FloatArray> = error("graph not used in these tests")

      override fun close() = Unit
    }
  private val classifier = VigilClassifier(spec, tokenizer, noGraph)

  @Test
  fun encodesRowsLikeThePythonReference() {
    for (case in golden("inference_golden.json", Array<InferenceCase>::class.java)) {
      val batch = classifier.encode(case.text)
      val s = spec.seqLen
      for ((row, want) in listOf(case.categoryIds, case.severityIds).withIndex()) {
        val length = batch.attentionMask.copyOfRange(row * s, (row + 1) * s).sum()
        assertArrayEquals(want, batch.inputIds.copyOfRange(row * s, row * s + length))
      }
    }
  }

  @Test
  fun keepsTheMostRecentTokensOfALongConversation() {
    val long = "Other: first message\n" + "Child: filler line\n".repeat(400) + "Other: LAST WORDS"
    val batch = classifier.encode(long)
    val s = spec.seqLen
    assertEquals(s, batch.attentionMask.copyOfRange(0, s).sum())
    val tail = tokenizer.encode("LAST WORDS")
    val row = batch.inputIds.copyOfRange(0, s)
    assertArrayEquals(tail, row.copyOfRange(s - 1 - tail.size, s - 1)) // the final id is the separator
    assertEquals(spec.sepId, row[s - 1])
  }

  @Test
  fun flagsACategoryOnlyAtItsThreshold() {
    // Option 0 is "benign", then the categories in spec order; all the mass goes to one option.
    val bullying = arrayOf(floatArrayOf(-40f, -40f, 0f, -40f, -40f, -40f, -40f, -40f), floatArrayOf(0f, 0f, 0f, 5f, 0f, 0f, 0f, 0f))
    val risky = classifier.decode(bullying)
    assertEquals(listOf("bullying"), risky.flagged)
    assertEquals("high", risky.riskLevel)

    val benign = arrayOf(floatArrayOf(0f, -40f, -40f, -40f, -40f, -40f, -40f, -40f), floatArrayOf(5f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
    val safe = classifier.decode(benign)
    assertTrue(safe.flagged.isEmpty())
    assertEquals("none", safe.riskLevel)
  }
}
