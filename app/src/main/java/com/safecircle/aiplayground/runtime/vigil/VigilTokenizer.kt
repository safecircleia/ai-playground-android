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

import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val SPACE = '▁' // "▁"

/**
 * mmBERT's BPE tokenizer (Gemma-2 style vocabulary) reading `vigil_tokenizer.bin`.
 *
 * Line-by-line port of `Bpe` in vigil/bpe.py, which is checked against the Hugging Face tokenizer:
 * spaces become "▁", a "▁" is prepended, the text is split before every "▁", unknown characters fall
 * back to `<0xXX>` byte tokens, and adjacent symbols merge by lowest rank. Added tokens such as
 * `<eos>` typed into the text are matched literally first, as `tokenizers` does.
 */
class VigilTokenizer private constructor(
  private val charCodepoints: IntArray,
  private val charIds: IntArray,
  private val byteIds: IntArray,
  private val added: List<Pair<String, Int>>,
  private val mergeKeys: LongArray,
  private val mergeRanks: IntArray,
  private val mergeResults: IntArray,
  private val unkId: Int,
) {
  private val addedFirstChars: Set<Char> = added.map { it.first[0] }.toSet()

  /** Token ids without special tokens, like `tok(text, add_special_tokens=False)["input_ids"]`. */
  fun encode(text: String): IntArray {
    val ids = IntList()
    var start = 0
    var i = 0
    while (i < text.length) {
      val hit =
        if (text[i] in addedFirstChars) added.firstOrNull { text.startsWith(it.first, i) } else null
      if (hit == null) {
        i++
        continue
      }
      segment(text.substring(start, i), ids)
      ids.add(hit.second)
      i += hit.first.length
      start = i
    }
    segment(text.substring(start), ids)
    return ids.toArray()
  }

  private fun segment(raw: String, out: IntList) {
    if (raw.isEmpty()) return
    var text = raw.replace(' ', SPACE)
    if (text[0] != SPACE) text = SPACE + text
    var start = 0
    for (i in 1 until text.length) {
      if (text[i] == SPACE) {
        piece(text, start, i, out)
        start = i
      }
    }
    piece(text, start, text.length, out)
  }

  private fun piece(text: String, from: Int, to: Int, out: IntList) {
    val symbols = IntList()
    var i = from
    while (i < to) {
      val cp = text.codePointAt(i)
      i += Character.charCount(cp)
      val slot = charCodepoints.binarySearch(cp)
      if (slot >= 0) {
        symbols.add(charIds[slot])
        continue
      }
      val bytes = String(Character.toChars(cp)).toByteArray(Charsets.UTF_8)
      if (bytes.any { byteIds[it.toInt() and 0xFF] < 0 }) {
        // Unknown characters fuse into one <unk>.
        if (symbols.size == 0 || symbols[symbols.size - 1] != unkId) symbols.add(unkId)
      } else {
        for (b in bytes) symbols.add(byteIds[b.toInt() and 0xFF])
      }
    }
    while (symbols.size > 1) {
      var bestAt = -1
      var bestRank = Int.MAX_VALUE
      var bestResult = 0
      for (k in 0 until symbols.size - 1) {
        val slot = mergeKeys.binarySearch((symbols[k].toLong() shl 32) or symbols[k + 1].toLong())
        if (slot >= 0 && mergeRanks[slot] < bestRank) {
          bestAt = k
          bestRank = mergeRanks[slot]
          bestResult = mergeResults[slot]
        }
      }
      if (bestAt < 0) break
      symbols.replacePairAt(bestAt, bestResult)
    }
    for (k in 0 until symbols.size) out.add(symbols[k])
  }

  companion object {
    fun parse(bytes: ByteArray): VigilTokenizer {
      val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
      val magic = ByteArray(4).also { buf.get(it) }
      require(String(magic, Charsets.US_ASCII) == "VTOK") { "not a Vigil tokenizer file" }
      val nChars = buf.int
      val nBytes = buf.int
      val nAdded = buf.int
      val nMerges = buf.int
      val unk = buf.int
      val cps = IntArray(nChars)
      val ids = IntArray(nChars)
      for (n in 0 until nChars) {
        cps[n] = buf.int
        ids[n] = buf.int
      }
      val byteIds = IntArray(nBytes) { buf.int }
      val added =
        List(nAdded) {
          val id = buf.int
          val len = buf.short.toInt() and 0xFFFF
          val raw = ByteArray(len).also { buf.get(it) }
          String(raw, Charsets.UTF_8) to id
        }
      val keys = LongArray(nMerges)
      val ranks = IntArray(nMerges)
      val results = IntArray(nMerges)
      for (n in 0 until nMerges) {
        keys[n] = buf.long
        ranks[n] = buf.int
        results[n] = buf.int
      }
      return VigilTokenizer(cps, ids, byteIds, added, keys, ranks, results, unk)
    }
  }
}

/** Growable int array; avoids boxing for the thousands of symbols a conversation produces. */
private class IntList {
  private var data = IntArray(64)
  var size = 0
    private set

  fun add(v: Int) {
    if (size == data.size) data = data.copyOf(size * 2)
    data[size++] = v
  }

  operator fun get(i: Int) = data[i]

  /** Replace the two symbols at [i] and [i] + 1 with one. */
  fun replacePairAt(i: Int, v: Int) {
    data[i] = v
    System.arraycopy(data, i + 2, data, i + 1, size - i - 2)
    size--
  }

  fun toArray() = data.copyOf(size)
}
