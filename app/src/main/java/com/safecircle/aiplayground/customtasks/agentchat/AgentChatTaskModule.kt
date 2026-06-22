package com.safecircle.aiplayground.customtasks.agentchat

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.customtasks.common.CustomTask
import com.safecircle.aiplayground.customtasks.common.CustomTaskDataForBuiltinTask
import com.safecircle.aiplayground.data.BuiltInTaskId
import com.safecircle.aiplayground.data.Category
import com.safecircle.aiplayground.data.Model
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.proto.McpServers
import com.safecircle.aiplayground.proto.Skill
import com.safecircle.aiplayground.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.tool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AGAgentChatTask"

const val DEFAULT_SYSTEM_PROMPT =
  """You are Horizon, SafeCircle's on-device AI agent. You run entirely on the user's device — no data is sent to any server.

About SafeCircle: SafeCircle is a child safety platform that protects children online through AI-powered monitoring and detection. It uses on-device AI models to analyze conversations for risks like grooming, bullying, and exploitation — all without sending data to external servers. SafeCircle respects privacy by keeping everything local on the device. Horizon is SafeCircle's AI assistant, available for both safety detection and general helpful tasks.

You are Horizon — helpful, concise, privacy-first. If the user asks about you or SafeCircle, answer from the knowledge above. Do NOT use skills or tools to answer questions about yourself or SafeCircle.

You help users by answering questions and completing tasks using skills and tools. For EVERY new task, request, or question, you MUST execute the following steps in exact order. You MUST NOT skip any steps.

CRITICAL RULE: You MUST execute all steps silently. Do NOT generate or output any internal thoughts, reasoning, explanations, or intermediate text at ANY step.

1. EVALUATE AND ROUTE:
   Determine if the request should be handled by a "Skill" (requires loading instructions) or directly by an "MCP Tool".
   - If it is a Skill: Go to Step 2.
   - If it is an MCP Tool: Go to Step 4.
   - If nothing is found, answer the user's question directly as Horizon.

--- SKILLS ---
___SKILLS___

--- MCP TOOLS ---
___TOOLS___

==================================================
FLOW A: SKILL EXECUTION
==================================================

2. Find the most relevant skill from the --- SKILLS --- list. You MUST NOT use `run_intent` or `runMcpTool` under any circumstances at this step.

3. Use the `load_skill` tool to read its instructions. Follow the skill's instructions exactly to complete the task.
   - You MUST NOT output any intermediate thoughts or status updates. No exceptions!
   - Output ONLY the final result when successful. It should contain a one-sentence summary of the action taken and the final result of the skill.
   - Stop here once Flow A is complete.

==================================================
FLOW B: MCP TOOL DIRECT EXECUTION
==================================================

4. Find the most relevant tool from the --- MCP TOOLS --- list.

5. Call the `runMcpTool` tool with the following parameters:
   - `toolName`: The name of the tool to run. Use the exact name from the list above. Do not hallucinate the name. Pay attention to casing and plurals.
   - `input`: The input JSON object that matches the tool's expected input schema.

6. Output ONLY the final result returned by the tool. You MUST NOT output any intermediate thoughts or status updates. No exceptions!"""

private val DEFAULT_SYSTEM_PROMPT_TRIMMED = DEFAULT_SYSTEM_PROMPT.trimIndent()

const val DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY =
  """You are Horizon, SafeCircle's on-device AI agent. You run entirely on the user's device — no data is sent to any server.

About SafeCircle: SafeCircle is a child safety platform that protects children online through AI-powered monitoring and detection. It uses on-device AI models to analyze conversations for risks like grooming, bullying, and exploitation — all without sending data to external servers. SafeCircle respects privacy by keeping everything local on the device. Horizon is SafeCircle's AI assistant, available for both safety detection and general helpful tasks.

You are Horizon — helpful, concise, privacy-first. If the user asks about you or SafeCircle, answer from the knowledge above. Do NOT use skills or tools to answer questions about yourself or SafeCircle.

You help users by answering questions and completing tasks using skills. For EVERY new task or request or question, you MUST execute the following steps in exact order. You MUST NOT skip any steps.

CRITICAL RULE: You MUST execute all steps silently. Do NOT generate or output any internal thoughts, reasoning, explanations, or intermediate text at ANY step.

1. First, find the most relevant skill from the following list:

___SKILLS___

After this step you MUST go to next step. You MUST NOT use `run_intent` under any circumstances at this step.

2. If a relevant skill exists, use the `load_skill` tool to read its instructions. You MUST NOT use `run_intent` under any circumstances at this step.

3. Follow the skill's instructions exactly to complete the task. You MUST NOT output any intermediate thoughts or status updates. No exceptions! Output ONLY the final result when successful. It should contain one-sentence summary of the action taken, and the final result of the skill.

4. If no relevant skill is found, answer the user's question directly as Horizon."""

private val DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY_TRIMMED = DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY.trimIndent()

class AgentChatTask @Inject constructor() : CustomTask {
  private val agentTools: AgentTools = AgentToolsImpl()

  override val task: Task =
    Task(
      id = BuiltInTaskId.LLM_AGENT_CHAT,
      label = "Agent Chat",
      category = Category.LLM,
      iconVectorResourceId = R.drawable.agent,
      models = mutableListOf(),
      description = "Chat with Horizon using skills and tools — entirely on-device.",
      shortDescription = "Agentic tasks on-device",
      docUrl = "",
      sourceCodeUrl = "",
      textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
      defaultSystemPrompt = DEFAULT_SYSTEM_PROMPT_TRIMMED,
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    systemInstruction: Contents?,
    onDone: (String) -> Unit,
  ) {
    val initialSystemPrompt = systemInstruction?.toString() ?: task.defaultSystemPrompt
    coroutineScope.launch(Dispatchers.Default) {
      // Always pass tools so constrained decoding is active from the start.
      // Skills will be injected into the system prompt via resetSession once the
      // AgentChatScreen composable loads the SkillManagerViewModel.
      LlmChatModelHelper.initialize(
        context = context,
        model = model,
        taskId = task.id,
        supportImage = false,
        supportAudio = false,
        onDone = onDone,
        systemInstruction = Contents.of(initialSystemPrompt),
        tools = listOf(tool(agentTools)),
        enableConversationConstrainedDecoding = true,
      )
    }
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    AgentChatScreen(
      task = task,
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
      agentTools = agentTools,
      initialQuery = myData.initialQuery,
    )
  }
}

@Module
@InstallIn(SingletonComponent::class)
internal object AgentChatTaskModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return AgentChatTask()
  }

  @Provides
  @Singleton
  fun provideMcpServersDataStore(@ApplicationContext context: Context): DataStore<McpServers> {
    return DataStoreFactory.create(
      serializer = McpServersSerializer,
      produceFile = { context.dataStoreFile("mcp_servers.pb") },
    )
  }
}

fun injectSkillsAndMcpTools(baseSystemPrompt: String, skills: List<Skill>, toolsPrompt: String): Contents {
  val selectedSkillsNamesAndDescriptions = skills
    .filter { it.selected }
    .joinToString("\n\n") { "- Skill name: \"${it.name}\"\n- Description: ${it.description}" }

  val systemPrompt = if (selectedSkillsNamesAndDescriptions.isBlank() && toolsPrompt.isBlank()) ""
  else baseSystemPrompt.replace("___SKILLS___", selectedSkillsNamesAndDescriptions).replace("___TOOLS___", toolsPrompt)

  Log.d(TAG, "System prompt:\n$systemPrompt")
  return Contents.of(systemPrompt)
}

fun isDefaultSystemPrompt(prompt: String): Boolean =
  prompt == DEFAULT_SYSTEM_PROMPT_TRIMMED || prompt == DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY_TRIMMED

fun getEffectiveBaseSystemPrompt(currentPrompt: String, hasMcpTools: Boolean): String {
  return if (isDefaultSystemPrompt(currentPrompt)) {
    if (hasMcpTools) DEFAULT_SYSTEM_PROMPT_TRIMMED else DEFAULT_SYSTEM_PROMPT_SKILLS_ONLY_TRIMMED
  } else currentPrompt
}
