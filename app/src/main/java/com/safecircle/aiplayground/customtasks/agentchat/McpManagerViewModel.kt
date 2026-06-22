package com.safecircle.aiplayground.customtasks.agentchat

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safecircle.aiplayground.proto.McpAuth
import com.safecircle.aiplayground.proto.McpServer
import com.safecircle.aiplayground.proto.McpServers
import com.safecircle.aiplayground.proto.McpTool
import com.safecircle.aiplayground.proto.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AGMcpManagerVM"

data class McpServerState(val mcpServer: McpServer, val client: Client?, val error: String? = null)

data class McpManagerUiState(
  val mcpServers: List<McpServerState> = emptyList(),
  val loadingMcpServer: Boolean = false,
  val error: String? = null,
)

@HiltViewModel
class McpManagerViewModel
@Inject
constructor(
  private val mcpServersDataStore: DataStore<McpServers>,
  private val userDataDataStore: DataStore<UserData>,
) : ViewModel() {
  private val _uiState = MutableStateFlow(McpManagerUiState())
  val uiState = _uiState.asStateFlow()

  private val httpClient = HttpClient(Android) { install(SSE) }

  suspend fun loadMcpServers() {
    _uiState.update { it.copy(loadingMcpServer = true) }
    withContext(Dispatchers.IO) {
      try {
        val savedServers = mcpServersDataStore.data.first().mcpServerList
        val loadedStates = savedServers.map { serverProto ->
          try {
            val savedToolsMap = serverProto.toolsList.associate { it.name to it.enabled }
            val savedAlwaysAllowMap = serverProto.toolsList.associate { it.name to it.alwaysAllow }
            val (client, mcpTools) = initializeClientAndLoadTools(serverProto.url, savedToolsMap, savedAlwaysAllowMap)
            val serverVersion = client.serverVersion
            val updatedServerProto = serverProto.toBuilder()
              .clearTools()
              .addAllTools(mcpTools)
              .setEnabled(serverProto.enabled)
              .apply {
                serverVersion?.name?.let { setName(it) }
                serverVersion?.version?.let { setVersion(it) }
                val desc = mcpTools.joinToString(", ") { it.name }
                if (desc.isNotEmpty()) setDescription("Tools: $desc")
              }
              .build()
            McpServerState(mcpServer = updatedServerProto, client = client, error = null)
          } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error loading MCP server: ${serverProto.url}", e)
            McpServerState(mcpServer = serverProto.toBuilder().setEnabled(false).build(), client = null, error = e.message ?: "Failed to connect")
          }
        }
        _uiState.update { it.copy(mcpServers = loadedStates, loadingMcpServer = false) }
        mcpServersDataStore.updateData {
          McpServers.newBuilder()
            .addAllMcpServer(loadedStates.mapIndexed { index, state ->
              if (state.error != null) savedServers[index] else state.mcpServer
            })
            .build()
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error reading saved MCP servers", e)
        _uiState.update { it.copy(loadingMcpServer = false) }
      }
    }
  }

  fun addMcpServer(url: String, authType: McpAuth.AuthMethodCase, headerName: String, headerValue: String) {
    _uiState.update { it.copy(loadingMcpServer = true, error = null) }
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val currentAuth = McpAuth.newBuilder().apply {
          when (authType) {
            McpAuth.AuthMethodCase.REQUEST_HEADER -> setRequestHeader(
              McpAuth.RequestHeader.newBuilder().setHeaderName(headerName).setHeaderValue(headerValue).build()
            )
            else -> setNone(true)
          }
        }.build()

        val (client, mcpTools) = initializeClientAndLoadTools(url, mcpAuth = currentAuth)
        val serverVersion = client.serverVersion
        val mcpServerProto = McpServer.newBuilder()
          .setUrl(url)
          .addAllTools(mcpTools)
          .setEnabled(true)
          .apply {
            serverVersion?.name?.let { setName(it) }
            serverVersion?.version?.let { setVersion(it) }
            val desc = mcpTools.joinToString(", ") { it.name }
            if (desc.isNotEmpty()) setDescription("Tools: $desc")
          }
          .build()

        val newState = McpServerState(mcpServer = mcpServerProto, client = client, error = null)

        mcpServersDataStore.updateData { currentServers ->
          val filtered = currentServers.mcpServerList.filter { it.url != url }
          McpServers.newBuilder().addAllMcpServer(filtered + mcpServerProto).build()
        }
        userDataDataStore.updateData { it.toBuilder().putMcpAuths(url, currentAuth).build() }
        _uiState.update { currentState ->
          currentState.copy(mcpServers = currentState.mcpServers.filter { it.mcpServer.url != url } + newState, loadingMcpServer = false)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error adding MCP server: $url", e)
        _uiState.update { it.copy(error = e.message ?: "Failed to connect", loadingMcpServer = false) }
      }
    }
  }

  fun clearError() { _uiState.update { it.copy(error = null) } }

  fun removeMcpServer(url: String) {
    viewModelScope.launch(Dispatchers.IO) {
      mcpServersDataStore.updateData { currentServers ->
        McpServers.newBuilder().addAllMcpServer(currentServers.mcpServerList.filter { it.url != url }).build()
      }
      _uiState.update { it.copy(mcpServers = it.mcpServers.filter { s -> s.mcpServer.url != url }) }
    }
  }

  fun hasMcpServer(url: String): Boolean = _uiState.value.mcpServers.any { it.mcpServer.url == url }

  fun getToolsPrompt(): String {
    return _uiState.value.mcpServers
      .filter { it.mcpServer.enabled }
      .flatMap { it.mcpServer.toolsList }
      .filter { it.enabled }
      .joinToString("\n\n") { tool ->
        "MCP tool name: \"${tool.name}\"\n- Description: ${tool.description}\n- Input schema: ${tool.inputSchema}"
      }
  }

  fun setMcpServerEnabled(url: String, enabled: Boolean) {
    _uiState.update { currentState ->
      val updatedServers = currentState.mcpServers.map { state ->
        if (state.mcpServer.url == url) state.copy(mcpServer = state.mcpServer.toBuilder().setEnabled(enabled).build())
        else state
      }
      persistServers(updatedServers)
      currentState.copy(mcpServers = updatedServers)
    }
  }

  fun setMcpToolEnabled(url: String, toolName: String, enabled: Boolean) {
    _uiState.update { currentState ->
      val updatedServers = currentState.mcpServers.map { state ->
        if (state.mcpServer.url == url) {
          val updatedTools = state.mcpServer.toolsList.map { tool ->
            if (tool.name == toolName) tool.toBuilder().setEnabled(enabled).build() else tool
          }
          state.copy(mcpServer = state.mcpServer.toBuilder().clearTools().addAllTools(updatedTools).build())
        } else state
      }
      persistServers(updatedServers)
      currentState.copy(mcpServers = updatedServers)
    }
  }

  fun setAllMcpServerEnabled(enabled: Boolean) {
    _uiState.update { currentState ->
      val updatedServers = currentState.mcpServers.map { state ->
        if (state.error != null) state
        else state.copy(mcpServer = state.mcpServer.toBuilder().setEnabled(enabled).build())
      }
      persistServers(updatedServers)
      currentState.copy(mcpServers = updatedServers)
    }
  }

  fun setAllMcpToolsEnabled(url: String, enabled: Boolean) {
    _uiState.update { currentState ->
      val updatedServers = currentState.mcpServers.map { state ->
        if (state.mcpServer.url == url) {
          val updatedTools = state.mcpServer.toolsList.map { it.toBuilder().setEnabled(enabled).build() }
          state.copy(mcpServer = state.mcpServer.toBuilder().clearTools().addAllTools(updatedTools).build())
        } else state
      }
      persistServers(updatedServers)
      currentState.copy(mcpServers = updatedServers)
    }
  }

  fun setMcpToolAlwaysAllow(url: String, toolName: String, alwaysAllow: Boolean) {
    _uiState.update { currentState ->
      val updatedServers = currentState.mcpServers.map { state ->
        if (state.mcpServer.url == url) {
          val updatedTools = state.mcpServer.toolsList.map { tool ->
            if (tool.name == toolName) tool.toBuilder().setAlwaysAllow(alwaysAllow).build() else tool
          }
          state.copy(mcpServer = state.mcpServer.toBuilder().clearTools().addAllTools(updatedTools).build())
        } else state
      }
      persistServers(updatedServers)
      currentState.copy(mcpServers = updatedServers)
    }
  }

  private suspend fun initializeClientAndLoadTools(
    url: String,
    savedToolsMap: Map<String, Boolean>? = null,
    savedAlwaysAllowMap: Map<String, Boolean>? = null,
    mcpAuth: McpAuth? = null,
  ): Pair<Client, List<McpTool>> {
    Log.d(TAG, "Initializing MCP for $url...")
    val client = Client(clientInfo = Implementation(name = "safecircle-aiplayground", version = "1.0.0"))
    val resolvedAuth = mcpAuth ?: userDataDataStore.data.first().mcpAuthsMap[url]
    val transport = if (resolvedAuth != null && resolvedAuth.authMethodCase == McpAuth.AuthMethodCase.REQUEST_HEADER) {
      val reqHeader = resolvedAuth.requestHeader
      StreamableHttpClientTransport(client = httpClient, url = url, requestBuilder = { headers.append(reqHeader.headerName, reqHeader.headerValue) })
    } else {
      StreamableHttpClientTransport(client = httpClient, url = url)
    }
    client.connect(transport)
    val toolsResponse = client.listTools()
    val mcpTools = toolsResponse?.tools.orEmpty().map { tool ->
      val isEnabled = savedToolsMap?.get(tool.name) ?: true
      val isAlwaysAllow = savedAlwaysAllowMap?.get(tool.name) ?: false
      val propertiesJson = tool.inputSchema.properties.toString()
      val requiredJson = tool.inputSchema.required?.joinToString(prefix = "[", postfix = "]") { "\"$it\"" } ?: "[]"
      val schemaJson = """{"type":"object","properties":$propertiesJson,"required":$requiredJson}"""
      McpTool.newBuilder()
        .setName(tool.name)
        .setDescription(tool.description ?: "")
        .setInputSchema(schemaJson)
        .setEnabled(isEnabled)
        .setAlwaysAllow(isAlwaysAllow)
        .build()
    }
    Log.d(TAG, "Loaded ${mcpTools.size} tools from $url")
    return Pair(client, mcpTools)
  }

  private fun persistServers(updatedServers: List<McpServerState>) {
    viewModelScope.launch(Dispatchers.IO) {
      mcpServersDataStore.updateData {
        McpServers.newBuilder().addAllMcpServer(updatedServers.map { state ->
          if (state.error != null) it.mcpServerList.find { s -> s.url == state.mcpServer.url } ?: state.mcpServer
          else state.mcpServer
        }).build()
      }
    }
  }
}
