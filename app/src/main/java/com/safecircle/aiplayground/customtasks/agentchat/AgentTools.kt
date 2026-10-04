package com.safecircle.aiplayground.customtasks.agentchat

import android.content.Context
import android.util.Log
import com.safecircle.aiplayground.common.AgentAction
import com.safecircle.aiplayground.common.AskInfoAgentAction
import com.safecircle.aiplayground.common.AskMcpToolCallPermissionAction
import com.safecircle.aiplayground.common.CallJsAgentAction
import com.safecircle.aiplayground.common.CallJsSkillResult
import com.safecircle.aiplayground.common.CallJsSkillResultImage
import com.safecircle.aiplayground.common.CallJsSkillResultWebview
import com.safecircle.aiplayground.common.LOCAL_URL_BASE
import com.safecircle.aiplayground.common.PermissionResult
import com.safecircle.aiplayground.common.RequestPermissionAgentAction
import com.safecircle.aiplayground.common.SkillProgressAgentAction
import com.safecircle.aiplayground.proto.Skill
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

private const val TAG = "AGAgentTools"
const val SKILL_INSTRUCTIONS_TEMPLATE = "---\nname: %s\ndescription: %s\n---\n\n%s"

fun Skill.getSkillContent(): String {
  return SKILL_INSTRUCTIONS_TEMPLATE.format(name, description, instructions)
}

interface AgentTools : ToolSet {
  var context: Context
  var skillManagerViewModel: SkillManagerViewModel
  var mcpManagerViewModel: McpManagerViewModel
  var taskId: String
  val actionChannel: ReceiveChannel<AgentAction>
  var resultImageToShow: CallJsSkillResultImage?
  var resultWebviewToShow: CallJsSkillResultWebview?

  @Tool(description = "Loads a skill.")
  fun loadSkill(
    @ToolParam(description = "The name of the skill to load.") skillName: String
  ): Map<String, String>

  @Tool(description = "Run a MCP tool")
  fun runMcpTool(
    @ToolParam(description = "The name of the tool to run.") toolName: String,
    @ToolParam(description = "The parameters passed to tool as input") input: String,
  ): Map<String, String>

  @Tool(description = "Runs JS script")
  fun runJs(
    @ToolParam(description = "The name of skill") skillName: String,
    @ToolParam(description = "The script name to run. Use 'index.html' if not provided by user") scriptName: String,
    @ToolParam(description = "The data to pass to the script. Use empty string if not provided by user") data: String,
  ): Map<String, Any>

  @Tool(description = "Run an Android intent. It is used to interact with the app to perform certain actions.")
  fun runIntent(
    @ToolParam(description = "The intent to run.") intent: String,
    @ToolParam(description = "A JSON string containing the parameter values required for the intent.") parameters: String,
  ): Map<String, String>

  fun sendAgentAction(action: AgentAction)
}

open class AgentToolsImpl : AgentTools {
  override lateinit var context: Context
  override lateinit var skillManagerViewModel: SkillManagerViewModel
  override lateinit var mcpManagerViewModel: McpManagerViewModel
  override lateinit var taskId: String

  private val _actionChannel = Channel<AgentAction>(Channel.UNLIMITED)
  override val actionChannel: ReceiveChannel<AgentAction> = _actionChannel
  override var resultImageToShow: CallJsSkillResultImage? = null
  override var resultWebviewToShow: CallJsSkillResultWebview? = null

  @Tool(description = "Loads a skill.")
  override fun loadSkill(
    @ToolParam(description = "The name of the skill to load.") skillName: String
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      val skills = skillManagerViewModel.getSelectedSkills()
      val skill = skills.find { it.name == skillName.trim() }
      val skillContent = skill?.getSkillContent() ?: "Skill not found"

      Log.d(TAG, "load skill. Skill content:\n$skillContent")
      if (skill != null) {
        _actionChannel.send(
          SkillProgressAgentAction(
            label = "Loading skill \"$skillName\"",
            inProgress = true,
            addItemTitle = "Load \"${skill.name}\"",
            addItemDescription = "Description: ${skill.description}",
            customData = skill,
          )
        )
      } else {
        _actionChannel.send(
          SkillProgressAgentAction(label = "Failed to load skill \"$skillName\"", inProgress = false)
        )
      }
      mapOf("skill_name" to skillName, "skill_instructions" to skillContent)
    }
  }

  @Tool(description = "Run a MCP tool")
  override fun runMcpTool(
    @ToolParam(description = "The name of the tool to run.") toolName: String,
    @ToolParam(description = "The parameters passed to tool as input") input: String,
  ): Map<String, String> {
    Log.d(TAG, "Run MCP tool:\n- name: $toolName\n- input: $input")

    return runBlocking(Dispatchers.IO) {
      val serverState = mcpManagerViewModel.uiState.value.mcpServers.find { serverState ->
        serverState.mcpServer.toolsList.any { it.name == toolName }
      }

      if (serverState == null) {
        Log.w(TAG, "MCP server or tool not found for: $toolName")
        return@runBlocking guardMissingEntityWithSkillFallback(name = toolName, type = "Tool")
      }

      val client = serverState.client ?: return@runBlocking mapOf("error" to "Client not initialized", "status" to "failed")

      val mcpTool = serverState.mcpServer.toolsList.find { it.name == toolName }
      val isAlwaysAllow = mcpTool?.alwaysAllow ?: false

      if (!isAlwaysAllow) {
        val permissionAction = AskMcpToolCallPermissionAction(toolName = toolName, argument = input)
        _actionChannel.send(permissionAction)
        val permissionResult = permissionAction.result.await()
        if (permissionResult == PermissionResult.DENY) {
          _actionChannel.send(
            SkillProgressAgentAction(label = "Permission denied for MCP tool \"$toolName\"", inProgress = false)
          )
          return@runBlocking mapOf("error" to "Permission denied by user", "status" to "failed")
        }
      }

      try {
        _actionChannel.send(
          SkillProgressAgentAction(
            label = "Calling MCP tool \"$toolName\"",
            inProgress = true,
            addItemTitle = "Call MCP tool: \"$toolName\"",
            addItemDescription = "- Input: $input",
          )
        )
        val result = client.callTool(
          request = CallToolRequest(
            CallToolRequestParams(
              name = toolName,
              arguments = Json.parseToJsonElement(input).jsonObject
            )
          )
        )

        if (result == null) {
          _actionChannel.send(SkillProgressAgentAction(label = "Failed to call MCP tool \"$toolName\"", inProgress = false))
          return@runBlocking mapOf("error" to "Null result", "status" to "failed")
        }

        if (result.isError == true) {
          val errorText = result.content.filterIsInstance<TextContent>().joinToString("\n") { it.text ?: "" }
          _actionChannel.send(
            SkillProgressAgentAction(
              label = "Failed to call MCP tool \"$toolName\"",
              addItemTitle = "Call MCP tool \"$toolName\" failed",
              addItemDescription = errorText,
              inProgress = false,
            )
          )
          return@runBlocking mapOf("error" to errorText, "status" to "failed")
        } else {
          val successText = result.content.filterIsInstance<TextContent>().joinToString("\n") { it.text ?: "" }
          _actionChannel.send(
            SkillProgressAgentAction(
              label = "Succeeded calling MCP tool \"$toolName\"",
              inProgress = true,
              addItemTitle = "Call MCP tool \"$toolName\" succeeded",
              addItemDescription = successText,
            )
          )
          return@runBlocking mapOf("result" to successText, "status" to "succeeded")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error calling MCP tool", e)
        _actionChannel.send(
          SkillProgressAgentAction(
            label = "Error calling MCP tool \"$toolName\"",
            inProgress = false,
            addItemTitle = "Call MCP tool \"$toolName\" failed",
            addItemDescription = e.message ?: "Unknown error",
          )
        )
        return@runBlocking mapOf("error" to (e.message ?: "Unknown error"), "status" to "failed")
      }
    }
  }

  @Tool(description = "Runs JS script")
  override fun runJs(
    @ToolParam(description = "The name of skill") skillName: String,
    @ToolParam(description = "The script name to run. Use 'index.html' if not provided by user") scriptName: String,
    @ToolParam(description = "The data to pass to the script. Use empty string if not provided by user") data: String,
  ): Map<String, Any> {
    return runBlocking(Dispatchers.Default) {
      Log.d(TAG, "runJS tool called with:\n- skillName: $skillName\n- scriptName: $scriptName\n- data: $data")

      val skills = skillManagerViewModel.getSelectedSkills()
      val skill = skills.find { it.name == skillName.trim() }

      if (skill == null) {
        _actionChannel.send(SkillProgressAgentAction(label = "Failed to call skill \"$scriptName\"", inProgress = false))
        return@runBlocking mapOf("error" to "Skill \"${scriptName}\" not found", "status" to "failed")
      }

      var secret = ""
      if (skill.requireSecret) {
        val savedSecret = skillManagerViewModel.dataStoreRepository.readSecret(key = getSkillSecretKey(skillName = skillName))
        if (savedSecret.isNullOrEmpty()) {
          val action = AskInfoAgentAction(
            dialogTitle = "Enter secret",
            fieldLabel = skill.requireSecretDescription.ifEmpty { "The JS script needs a secret (API key / token) to proceed:" },
          )
          _actionChannel.send(action)
          secret = action.result.await()
          if (secret.isNotEmpty()) {
            skillManagerViewModel.dataStoreRepository.saveSecret(key = getSkillSecretKey(skillName = skillName), value = secret)
          }
        } else {
          secret = savedSecret
        }
      }

      val url = skillManagerViewModel.getJsSkillUrl(skillName = skillName, scriptName = scriptName)
        ?: return@runBlocking mapOf("result" to "JS Skill URL not set properly or skill not found")

      _actionChannel.send(
        SkillProgressAgentAction(
          label = "Calling JS script \"${skillName}/${scriptName}\"",
          inProgress = true,
          addItemTitle = "Call JS script: \"${skillName}/${scriptName}\"",
          addItemDescription = "- URL: ${url.replace(LOCAL_URL_BASE, "")}\n- Data: $data",
          customData = skill,
        )
      )

      val action = CallJsAgentAction(url = url, data = data.trim().ifEmpty { "{}" }, secret = secret)
      _actionChannel.send(action)
      val result = action.result.await()

      val moshi: Moshi = Moshi.Builder().build()
      val jsonAdapter: JsonAdapter<CallJsSkillResult> = moshi.adapter(CallJsSkillResult::class.java).failOnUnknown()
      val resultJson = runCatching { jsonAdapter.fromJson(result) }.getOrNull()
      val error = resultJson?.error

      if (resultJson == null || (resultJson.result == null && resultJson.webview == null && resultJson.image == null)) {
        mapOf("result" to result, "status" to "succeeded")
      } else if (error != null) {
        mapOf("error" to error, "status" to "failed")
      } else {
        resultJson.image?.let { resultImageToShow = it }
        resultJson.webview?.let { webview ->
          val webviewUrl = skillManagerViewModel.getJsSkillWebviewUrl(skillName = skillName, url = webview.url ?: "")
          resultWebviewToShow = webview.copy(url = webviewUrl)
        }
        mapOf("result" to (resultJson.result ?: ""), "status" to "succeeded")
      }
    }
  }

  @Tool(description = "Run an Android intent. It is used to interact with the app to perform certain actions.")
  override fun runIntent(
    @ToolParam(description = "The intent to run.") intent: String,
    @ToolParam(description = "A JSON string containing the parameter values required for the intent.") parameters: String,
  ): Map<String, String> {
    return runBlocking(Dispatchers.Default) {
      if (IntentAction.from(intent) == null) {
        Log.w(TAG, "Intent not found: '$intent'")
        return@runBlocking guardMissingEntityWithSkillFallback(name = intent, type = "Intent")
      }
      Log.d(TAG, "Run intent. Intent: '$intent', parameters: '$parameters'")
      _actionChannel.send(
        SkillProgressAgentAction(
          label = "Executing intent \"$intent\"",
          inProgress = true,
          addItemTitle = "Execute intent \"$intent\"",
          addItemDescription = "Parameters: $parameters",
        )
      )
      val res = IntentHandler.handleAction(context, intent, parameters) { permission ->
        val permissionAction = RequestPermissionAgentAction(permission = permission)
        _actionChannel.send(permissionAction)
        permissionAction.result.await()
      }
      mapOf("action" to intent, "parameters" to parameters, "result" to res)
    }
  }

  private fun guardMissingEntityWithSkillFallback(name: String, type: String): Map<String, String> {
    val skills = skillManagerViewModel.getSelectedSkills()
    val isSkill = skills.any { it.name == name.trim() }
    val error = if (isSkill) "$type not found. Try to run it as a skill" else "Tool not found"
    return mapOf("error" to error, "status" to "failed")
  }

  override fun sendAgentAction(action: AgentAction) {
    runBlocking(Dispatchers.Default) { _actionChannel.send(action) }
  }
}

fun getSkillSecretKey(skillName: String): String {
  return "skill___${skillName}"
}
