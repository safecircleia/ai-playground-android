package com.safecircle.aiplayground.customtasks.agentchat

import android.content.Context
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.common.AskInfoAgentAction
import com.safecircle.aiplayground.common.AskMcpToolCallPermissionAction
import com.safecircle.aiplayground.common.CallJsAgentAction
import com.safecircle.aiplayground.common.LOCAL_URL_BASE
import com.safecircle.aiplayground.common.PermissionResult
import com.safecircle.aiplayground.common.RequestPermissionAgentAction
import com.safecircle.aiplayground.common.SkillProgressAgentAction
import com.safecircle.aiplayground.data.BuiltInTaskId
import com.safecircle.aiplayground.data.Model
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.ui.common.BaseGalleryWebViewClient
import com.safecircle.aiplayground.ui.common.GalleryWebView
import com.safecircle.aiplayground.ui.common.chat.ChatMessage
import com.safecircle.aiplayground.ui.common.chat.ChatMessageCollapsableProgressPanel
import com.safecircle.aiplayground.ui.common.chat.ChatMessageImage
import com.safecircle.aiplayground.ui.common.chat.ChatMessageText
import com.safecircle.aiplayground.ui.common.chat.ChatMessageType
import com.safecircle.aiplayground.ui.common.chat.ChatMessageWebView
import com.safecircle.aiplayground.ui.common.chat.ChatSide
import com.safecircle.aiplayground.ui.common.chat.LogMessage
import com.safecircle.aiplayground.ui.common.chat.LogMessageLevel
import com.safecircle.aiplayground.ui.common.chat.SendMessageTrigger
import com.safecircle.aiplayground.ui.llmchat.LlmChatScreen
import com.safecircle.aiplayground.ui.llmchat.LlmChatViewModel
import com.safecircle.aiplayground.ui.modelmanager.ModelInitializationStatusType
import com.safecircle.aiplayground.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

private const val TAG = "AGAgentChatScreen"
private val chatViewJavascriptInterface = ChatWebViewJavascriptInterface()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentChatScreen(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  agentTools: AgentTools,
  viewModel: LlmChatViewModel = hiltViewModel(),
  skillManagerViewModel: SkillManagerViewModel = hiltViewModel(),
  mcpManagerViewModel: McpManagerViewModel = hiltViewModel(),
  initialQuery: String? = null,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  agentTools.context = context
  agentTools.skillManagerViewModel = skillManagerViewModel
  agentTools.mcpManagerViewModel = mcpManagerViewModel
  agentTools.taskId = task.id
  val density = LocalDensity.current
  val windowInfo = LocalWindowInfo.current
  val screenWidthDp = remember { with(density) { windowInfo.containerSize.width.toDp() } }
  var showSkillManagerBottomSheet by remember { mutableStateOf(false) }
  var showMcpManagerBottomSheet by remember { mutableStateOf(false) }
  var showAskInfoDialog by remember { mutableStateOf(false) }
  var currentAskInfoAction by remember { mutableStateOf<AskInfoAgentAction?>(null) }
  var currentMcpPermissionAction by remember { mutableStateOf<AskMcpToolCallPermissionAction?>(null) }
  var askInfoInputValue by remember { mutableStateOf("") }
  var webViewRef: WebView? by remember { mutableStateOf(null) }
  val chatWebViewClient = remember { ChatWebViewClient(context = context) }
  var curSystemPrompt by remember { mutableStateOf(task.defaultSystemPrompt) }
  val systemPromptUpdatedMessage = stringResource(R.string.system_prompt_updated)
  var sendMessageTrigger by remember { mutableStateOf<SendMessageTrigger?>(null) }

  var currentPermissionAction by remember { mutableStateOf<RequestPermissionAgentAction?>(null) }
  val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    currentPermissionAction?.result?.complete(granted)
    currentPermissionAction = null
  }

  LaunchedEffect(task) { viewModel.loadSystemPrompt(task) }
  val uiSystemPrompt by viewModel.uiSystemPrompt.collectAsState()

  val llmChatUiState by viewModel.uiState.collectAsState()
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val skillUiState by skillManagerViewModel.uiState.collectAsState()
  val mcpUiState by mcpManagerViewModel.uiState.collectAsState()

  val skillCount = skillUiState.skills.count { it.skill.selected }
  val mcpCount = mcpUiState.mcpServers.count { it.mcpServer.enabled }
  val mcpToolsCount = mcpUiState.mcpServers.filter { it.mcpServer.enabled }.sumOf { it.mcpServer.toolsList.count { tool -> tool.enabled } }

  LaunchedEffect(uiSystemPrompt, mcpToolsCount) {
    curSystemPrompt = getEffectiveBaseSystemPrompt(uiSystemPrompt, mcpToolsCount > 0)
  }

  val selectedModel = modelManagerUiState.selectedModel
  val modelInitStatus = modelManagerUiState.modelInitializationStatus[selectedModel.name]

  // Load skills and reset session with them once model is initialized.
  var skillsInjected by remember { mutableStateOf(false) }
  LaunchedEffect(modelInitStatus?.status, selectedModel.name) {
    if (modelInitStatus?.status == ModelInitializationStatusType.INITIALIZED && !skillsInjected) {
      skillsInjected = true
      skillManagerViewModel.loadSkills()
      mcpManagerViewModel.loadMcpServers()
      resetSessionWithCurrentSkillsAndMcps(viewModel, modelManagerViewModel, skillManagerViewModel, task, curSystemPrompt, agentTools, clearHistory = false)
    }
  }

  var initialQueryConsumed by remember { mutableStateOf(false) }
  LaunchedEffect(llmChatUiState.isResettingSession, modelInitStatus?.status, selectedModel.name, initialQuery) {
    if (!initialQuery.isNullOrEmpty() && !initialQueryConsumed && modelInitStatus?.status == ModelInitializationStatusType.INITIALIZED && !llmChatUiState.isResettingSession) {
      initialQueryConsumed = true
      sendMessageTrigger = SendMessageTrigger(model = selectedModel, messages = listOf(ChatMessageText(content = initialQuery, side = ChatSide.USER)))
    }
  }

  LlmChatScreen(
    modelManagerViewModel = modelManagerViewModel,
    taskId = BuiltInTaskId.LLM_AGENT_CHAT,
    navigateUp = navigateUp,
    skillCount = skillCount,
    mcpCount = mcpCount,
    mcpToolsCount = mcpToolsCount,
    onFirstToken = { model -> scope.launch(Dispatchers.Main) { updateProgressPanel(viewModel, model, agentTools) } },
    onGenerateResponseDone = { model ->
      scope.launch(Dispatchers.Main) {
        agentTools.resultImageToShow?.let { resultImage ->
          resultImage.base64?.let { base64 ->
            decodeBase64ToBitmap(base64)?.let { bitmap ->
              viewModel.addMessage(model = model, message = ChatMessageImage(
                bitmaps = listOf(bitmap), imageBitMaps = listOf(bitmap.asImageBitmap()),
                side = ChatSide.AGENT, maxSize = (screenWidthDp.value * 0.8).toInt(), latencyMs = -1.0f, hideSenderLabel = true,
              ))
            }
          }
          agentTools.resultImageToShow = null
        }
        agentTools.resultWebviewToShow?.let { webview ->
          viewModel.addMessage(model = model, message = ChatMessageWebView(
            url = webview.url ?: "", iframe = webview.iframe == true, aspectRatio = webview.aspectRatio ?: 1.333f, hideSenderLabel = true,
          ))
          agentTools.resultWebviewToShow = null
        }
        updateProgressPanel(viewModel, model, agentTools)
      }
    },
    onSkillClicked = { showSkillManagerBottomSheet = true },
    onMcpClicked = { showMcpManagerBottomSheet = true },
    onResetSessionClickedOverride = { _, _, initialMessages, clearHistory, onDone ->
      resetSessionWithCurrentSkillsAndMcps(viewModel, modelManagerViewModel, skillManagerViewModel, task, curSystemPrompt, agentTools, onDone = { onDone() }, initialMessages = initialMessages, clearHistory = clearHistory)
    },
    showImagePicker = false,
    showAudioPicker = false,
    getActiveSkills = { skillManagerViewModel.getSelectedSkills().map { skillManagerViewModel.getSkillShortId(it) } },
    composableBelowMessageList = { model ->
      val actionChannel = agentTools.actionChannel
      val doneIcon = ImageVector.vectorResource(R.drawable.agent)
      val currentModel by androidx.compose.runtime.rememberUpdatedState(model)
      LaunchedEffect(actionChannel) {
        for (action in actionChannel) {
          when (action) {
            is SkillProgressAgentAction -> {
              viewModel.updateCollapsableProgressPanelMessage(
                model = currentModel, title = action.label, inProgress = action.inProgress,
                doneIcon = doneIcon, addItemTitle = action.addItemTitle,
                addItemDescription = action.addItemDescription, customData = action.customData,
              )
            }
            is CallJsAgentAction -> {
              val skillName = if (action.url.contains("/skills/")) action.url.substringAfter("/skills/").substringBefore("/")
              else if (action.url.startsWith("$LOCAL_URL_BASE/")) action.url.substringAfter("$LOCAL_URL_BASE/").substringBefore("/")
              else action.url
              try {
                launch {
                  delay(60000L)
                  if (!action.result.isCompleted) action.result.complete("{\"error\": \"Skill execution timed out.\"}")
                }
                suspendCancellableCoroutine<Unit> { continuation ->
                  chatWebViewClient.setPageLoadListener { chatWebViewClient.setPageLoadListener(null); continuation.resume(Unit) }
                  webViewRef?.loadUrl(action.url)
                }
                chatViewJavascriptInterface.onResultListener = { result -> action.result.complete(result) }
                val safeData = JSONObject.quote(action.data)
                val safeSecret = JSONObject.quote(action.secret)
                val script = """(async function(){var s=Date.now();while(true){if(typeof ai_edge_gallery_get_result==='function')break;await new Promise(r=>setTimeout(r,100));if(Date.now()-s>10000)break;}var result=await ai_edge_gallery_get_result($safeData,$safeSecret);AiEdgeGallery.onResultReady(result);})()"""
                webViewRef?.evaluateJavascript(script, null)
              } catch (e: Exception) { action.result.completeExceptionally(e) }
            }
            is AskInfoAgentAction -> { currentAskInfoAction = action; askInfoInputValue = ""; showAskInfoDialog = true }
            is RequestPermissionAgentAction -> { currentPermissionAction = action; permissionLauncher.launch(action.permission) }
            is AskMcpToolCallPermissionAction -> { currentMcpPermissionAction = action }
          }
        }
      }

      GalleryWebView(
        modifier = Modifier.size(1.dp),
        onWebViewCreated = { webView -> webViewRef = webView; webView.addJavascriptInterface(chatViewJavascriptInterface, "AiEdgeGallery") },
        customWebViewClient = chatWebViewClient,
        onConsoleMessage = { consoleMessage ->
          consoleMessage?.let { msg ->
            viewModel.addLogMessageToLastCollapsableProgressPanel(
              model = model,
              logMessage = LogMessage(
                level = when (msg.messageLevel()) {
                  ConsoleMessage.MessageLevel.ERROR -> LogMessageLevel.Error
                  ConsoleMessage.MessageLevel.WARNING -> LogMessageLevel.Warning
                  else -> LogMessageLevel.Info
                },
                source = msg.sourceId(), lineNumber = msg.lineNumber(), message = msg.message(),
              ),
            )
          }
        },
      )
    },
    allowEditingSystemPrompt = true,
    curSystemPrompt = curSystemPrompt,
    onSystemPromptChanged = { newPrompt ->
      curSystemPrompt = newPrompt
      viewModel.applySystemPromptChange(task = task, model = modelManagerViewModel.uiState.value.selectedModel, newPrompt = newPrompt, systemPromptUpdatedMessage = systemPromptUpdatedMessage)
    },
    emptyStateComposable = {
      Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(!WindowInsets.isImeVisible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.padding(horizontal = 48.dp).padding(bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text("Agent Chat", style = MaterialTheme.typography.headlineMedium)
              Text("Use skills and MCP tools with Horizon on-device. Manage skills and MCP servers from the toolbar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
          }
        }
      }
    },
    sendMessageTrigger = sendMessageTrigger,
  )

  if (showAskInfoDialog && currentAskInfoAction != null) {
    val action = currentAskInfoAction!!
    AlertDialog(
      onDismissRequest = { action.result.complete(""); showAskInfoDialog = false; currentAskInfoAction = null },
      title = { Text(action.dialogTitle) },
      text = {
        Column {
          Text(action.fieldLabel, style = MaterialTheme.typography.bodyMedium)
          OutlinedTextField(value = askInfoInputValue, onValueChange = { askInfoInputValue = it }, singleLine = true)
        }
      },
      confirmButton = { Button(onClick = { action.result.complete(askInfoInputValue); showAskInfoDialog = false; currentAskInfoAction = null }) { Text("OK") } },
      dismissButton = { TextButton(onClick = { action.result.complete(""); showAskInfoDialog = false; currentAskInfoAction = null }) { Text("Cancel") } },
    )
  }

  if (currentMcpPermissionAction != null) {
    val action = currentMcpPermissionAction!!
    AlertDialog(
      onDismissRequest = { action.result.complete(PermissionResult.DENY); currentMcpPermissionAction = null },
      title = { Text("Allow MCP tool call?") },
      text = { Text("Tool: ${action.toolName}\nInput: ${action.argument}") },
      confirmButton = { Button(onClick = { action.result.complete(PermissionResult.ALLOW_ONCE); currentMcpPermissionAction = null }) { Text("Allow") } },
      dismissButton = { TextButton(onClick = { action.result.complete(PermissionResult.DENY); currentMcpPermissionAction = null }) { Text("Deny") } },
    )
  }

  if (showSkillManagerBottomSheet) {
    SkillManagerBottomSheet(
      agentTools = agentTools,
      skillManagerViewModel = skillManagerViewModel,
      onDismiss = { selectedSkillsChanged ->
        showSkillManagerBottomSheet = false
        if (selectedSkillsChanged) {
          resetSessionWithCurrentSkillsAndMcps(viewModel, modelManagerViewModel, skillManagerViewModel, task, curSystemPrompt, agentTools)
        }
      },
    )
  }

  if (showMcpManagerBottomSheet) {
    McpManagerBottomSheet(
      mcpManagerViewModel = mcpManagerViewModel,
      onDismiss = { selectedChanged ->
        showMcpManagerBottomSheet = false
        if (selectedChanged) {
          resetSessionWithCurrentSkillsAndMcps(viewModel, modelManagerViewModel, skillManagerViewModel, task, curSystemPrompt, agentTools)
        }
      },
    )
  }
}

private fun updateProgressPanel(viewModel: LlmChatViewModel, model: Model, agentTools: AgentTools) {
  val lastProgressPanelMessage = viewModel.getLastMessageWithType(model = model, type = ChatMessageType.COLLAPSABLE_PROGRESS_PANEL)
  if (lastProgressPanelMessage != null && lastProgressPanelMessage is ChatMessageCollapsableProgressPanel) {
    val newLabel = when {
      lastProgressPanelMessage.title.startsWith("Loading") -> lastProgressPanelMessage.title.replace("Loading", "Loaded")
      lastProgressPanelMessage.title.startsWith("Calling") -> lastProgressPanelMessage.title.replace("Calling", "Called")
      lastProgressPanelMessage.title.startsWith("Executing") -> lastProgressPanelMessage.title.replace("Executing", "Executed")
      else -> lastProgressPanelMessage.title
    }
    agentTools.sendAgentAction(SkillProgressAgentAction(label = newLabel, inProgress = false))
  }
}

private fun resetSessionWithCurrentSkillsAndMcps(
  viewModel: LlmChatViewModel,
  modelManagerViewModel: ModelManagerViewModel,
  skillManagerViewModel: SkillManagerViewModel,
  task: Task,
  curSystemPrompt: String,
  agentTools: AgentTools,
  onDone: (Model) -> Unit = {},
  initialMessages: List<ChatMessage> = listOf(),
  clearHistory: Boolean = true,
) {
  val model = modelManagerViewModel.uiState.value.selectedModel
  val litertMessages = initialMessages.mapNotNull { chatMessage ->
    if (chatMessage is ChatMessageText) {
      if (chatMessage.side == ChatSide.USER) Message.user(chatMessage.content)
      else Message.model(chatMessage.content)
    } else null
  }
  val toolsPrompt = agentTools.mcpManagerViewModel.getToolsPrompt()
  val actualSystemPrompt = getEffectiveBaseSystemPrompt(curSystemPrompt, toolsPrompt.isNotEmpty())
  val skills = skillManagerViewModel.getSelectedSkills()
  Log.d(TAG, "resetSession: ${skills.size} skills loaded, tools passed, constrainedDecoding=true")
  viewModel.resetSession(
    task = task, model = model,
    systemInstruction = injectSkillsAndMcpTools(baseSystemPrompt = actualSystemPrompt, skills = skills, toolsPrompt = toolsPrompt),
    tools = listOf(tool(agentTools)), supportImage = false, supportAudio = false,
    onDone = { onDone(model) }, enableConversationConstrainedDecoding = true,
    initialMessages = litertMessages, clearHistory = clearHistory,
  )
}

class ChatWebViewJavascriptInterface {
  var onResultListener: ((String) -> Unit)? = null
  @JavascriptInterface fun onResultReady(result: String) { onResultListener?.invoke(result) }
}

class ChatWebViewClient(val context: Context) : BaseGalleryWebViewClient(context = context) {
  private var onPageLoaded: (() -> Unit)? = null
  fun setPageLoadListener(listener: (() -> Unit)?) { onPageLoaded = listener }
  override fun onPageFinished(view: WebView?, url: String?) { super.onPageFinished(view, url); onPageLoaded?.invoke() }
}
