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

import java.io.File
import java.io.RandomAccessFile
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

private const val MAX_MARKERS = 8
private const val ROWS = 2

/**
 * The exported Vigil encoder on the LiteRT CPU runtime. The model file is memory-mapped, so only the
 * embedding rows a conversation actually uses are paged in, not the whole 1.2 GB file.
 */
class LiteRtVigilGraph(modelFile: File, numThreads: Int = 4) : VigilGraph {
  private val interpreter: Interpreter

  init {
    val mapped =
      RandomAccessFile(modelFile, "r").use { it.channel.map(FileChannel.MapMode.READ_ONLY, 0, it.length()) }
    interpreter = Interpreter(mapped, Interpreter.Options().setNumThreads(numThreads))
  }

  // The interpreter is not thread-safe.
  @Synchronized
  override fun run(batch: VigilBatch): Array<FloatArray> {
    val inputs: Array<Any> =
      arrayOf(
        intBuffer(batch.inputIds),
        intBuffer(batch.attentionMask),
        intBuffer(batch.markerPos),
        intBuffer(batch.markerMask),
        intBuffer(batch.qtype),
      )
    val logits = Array(ROWS) { FloatArray(MAX_MARKERS) }
    interpreter.runForMultipleInputsOutputs(inputs, mapOf(0 to logits))
    return logits
  }

  @Synchronized override fun close() = interpreter.close()

  private fun intBuffer(values: IntArray): Buffer =
    ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer().apply {
      put(values)
      rewind()
    }
}
