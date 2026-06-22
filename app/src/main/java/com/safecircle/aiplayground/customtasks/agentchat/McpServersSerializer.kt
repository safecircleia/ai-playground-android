package com.safecircle.aiplayground.customtasks.agentchat

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.safecircle.aiplayground.proto.McpServers
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object McpServersSerializer : Serializer<McpServers> {
  override val defaultValue: McpServers = McpServers.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): McpServers {
    try {
      return McpServers.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read mcp servers proto.", exception)
    }
  }

  override suspend fun writeTo(t: McpServers, output: OutputStream) = t.writeTo(output)
}
