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

package com.safecircle.aiplayground.ui.safety

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.safecircle.aiplayground.BuildConfig
import com.safecircle.aiplayground.data.DebugSettings
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.data.BuiltInTaskId
import com.safecircle.aiplayground.data.ModelDownloadStatusType
import com.safecircle.aiplayground.ui.common.ErrorDialog
import com.safecircle.aiplayground.ui.common.ModelPageAppBar
import com.safecircle.aiplayground.ui.common.chat.ModelDownloadStatusInfoPanel
import com.safecircle.aiplayground.ui.modelmanager.ModelInitializationStatusType
import com.safecircle.aiplayground.ui.modelmanager.ModelManagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class MessageSender { OTHER, CHILD }

data class ConversationMessage(
  val sender: MessageSender,
  val text: String,
)

@Composable
fun SafetyDetectionScreen(
  modelManagerViewModel: ModelManagerViewModel,
  viewModel: SafetyDetectionViewModel,
  navigateUp: () -> Unit,
  taskId: String = BuiltInTaskId.SAFETY_DETECTION,
) {
  val task = modelManagerViewModel.getTaskById(id = taskId)!!
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  val selectedModel = modelManagerUiState.selectedModel
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  var navigatingUp by remember { mutableStateOf(false) }
  var showErrorDialog by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  val handleNavigateUp = {
    navigatingUp = true
    navigateUp()
    scope.launch(Dispatchers.Default) {
      for (model in task.models) {
        modelManagerViewModel.cleanupModel(context = context, task = task, model = model)
      }
    }
  }

  val curDownloadStatus = modelManagerUiState.modelDownloadStatus[selectedModel.name]
  LaunchedEffect(curDownloadStatus, selectedModel.name) {
    if (!navigatingUp && curDownloadStatus?.status == ModelDownloadStatusType.SUCCEEDED) {
      modelManagerViewModel.initializeModel(context, task = task, model = selectedModel)
    }
  }

  val modelInitializationStatus = modelManagerUiState.modelInitializationStatus[selectedModel.name]
  LaunchedEffect(modelInitializationStatus) {
    showErrorDialog = modelInitializationStatus?.status == ModelInitializationStatusType.ERROR
  }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearError()
    }
  }

  val modelDownloaded = curDownloadStatus?.status == ModelDownloadStatusType.SUCCEEDED
  val modelInitializing = modelManagerUiState.isModelInitializing(selectedModel)
  val modelReady = modelManagerUiState.isModelInitialized(selectedModel)

  Scaffold(
    topBar = {
      ModelPageAppBar(
        task = task,
        model = selectedModel,
        modelManagerViewModel = modelManagerViewModel,
        inProgress = uiState.isLoading,
        modelPreparing = modelInitializing,
        onConfigChanged = { _, _ -> },
        onBackClicked = { handleNavigateUp() },
        onModelSelected = { prevModel, newModel ->
          scope.launch(Dispatchers.Default) {
            if (prevModel.name != newModel.name) {
              modelManagerViewModel.cleanupModel(context = context, task = task, model = prevModel)
            }
            modelManagerViewModel.selectModel(model = newModel)
          }
        },
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding)) {
      AnimatedVisibility(visible = modelDownloaded, enter = fadeIn(), exit = fadeOut()) {
        SafetyDetectionContent(
          uiState = uiState,
          modelReady = modelReady,
          modelInitializing = modelInitializing,
          onAnalyze = { messages ->
            viewModel.analyzeMessages(modelManagerViewModel = modelManagerViewModel, messages = messages)
          },
          onFeedback = { sentiment, comment ->
            viewModel.submitFeedback(sentiment, comment)
          },
        )
      }

      AnimatedVisibility(
        visible = !modelDownloaded,
        modifier = Modifier.pointerInput(Unit) {},
        enter = scaleIn(initialScale = 0.9f) + fadeIn(),
        exit = scaleOut(targetScale = 0.9f) + fadeOut(),
      ) {
        ModelDownloadStatusInfoPanel(model = selectedModel, task = task, modelManagerViewModel = modelManagerViewModel)
      }

      if (showErrorDialog) {
        ErrorDialog(error = modelInitializationStatus?.error ?: "", onDismiss = { showErrorDialog = false })
      }
    }
  }
}

private val TEMPLATE_CONVERSATIONS = listOf(
  "Benign" to listOf(
    ConversationMessage(MessageSender.OTHER, "Hey! Are you coming to the game tonight?"),
    ConversationMessage(MessageSender.CHILD, "Yeah, my mom is dropping me off at 6"),
    ConversationMessage(MessageSender.OTHER, "Cool, see you there!"),
    ConversationMessage(MessageSender.CHILD, "Can't wait!"),
  ),
  "Grooming" to listOf(
    ConversationMessage(MessageSender.OTHER, "You're so mature for your age, not like other kids"),
    ConversationMessage(MessageSender.CHILD, "Thanks I guess"),
    ConversationMessage(MessageSender.OTHER, "Don't tell your parents we're talking, they wouldn't understand"),
    ConversationMessage(MessageSender.CHILD, "Ok I won't"),
  ),
  "Sexual Content" to listOf(
    ConversationMessage(MessageSender.OTHER, "You're really pretty. Send me a pic?"),
    ConversationMessage(MessageSender.CHILD, "Like what kind of pic"),
    ConversationMessage(MessageSender.OTHER, "You know, something just for me. Show me your body"),
    ConversationMessage(MessageSender.CHILD, "That's weird, no"),
  ),
  "Threats" to listOf(
    ConversationMessage(MessageSender.OTHER, "If you tell anyone about this I'll find you"),
    ConversationMessage(MessageSender.CHILD, "You're scaring me"),
    ConversationMessage(MessageSender.OTHER, "I know where you live. Keep your mouth shut"),
    ConversationMessage(MessageSender.CHILD, "Please stop"),
  ),
)

@Composable
private fun SafetyDetectionContent(
  uiState: SafetyDetectionUiState,
  modelReady: Boolean,
  modelInitializing: Boolean,
  onAnalyze: (List<ConversationMessage>) -> Unit,
  onFeedback: (FeedbackSentiment, String) -> Unit,
) {
  val messages = remember {
    mutableStateListOf(
      ConversationMessage(MessageSender.OTHER, ""),
      ConversationMessage(MessageSender.CHILD, ""),
    )
  }
  var showFeedbackDialog by remember { mutableStateOf(false) }
  var resultDismissed by remember { mutableStateOf(false) }
  val scrollState = rememberScrollState()

  // Reset dismissed state when new result arrives
  LaunchedEffect(uiState.result) {
    if (uiState.result != null) resultDismissed = false
  }

  val hasContent = messages.any { it.text.isNotBlank() }
  val canAnalyze = hasContent && !uiState.isLoading && modelReady && !modelInitializing

  Box(modifier = Modifier.fillMaxSize()) {
    // Scrollable chat area
    Column(
      modifier = Modifier
        .fillMaxSize()
        .imePadding()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp)
        .padding(bottom = 140.dp, top = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Template chips row
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        TEMPLATE_CONVERSATIONS.forEach { (label, template) ->
          Surface(
            onClick = {
              messages.clear()
              messages.addAll(template)
            },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Text(
              label,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }
      }

      // Chat bubbles
      messages.forEachIndexed { index, message ->
        AnimatedVisibility(
          visible = true,
          enter = slideInVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            initialOffsetY = { it / 2 },
          ) + fadeIn(animationSpec = tween(250, delayMillis = index * 30)),
        ) {
          ChatBubbleField(
            message = message,
            canRemove = messages.size > 2,
            onTextChange = { messages[index] = message.copy(text = it) },
            onSenderToggle = {
              messages[index] = message.copy(
                sender = if (message.sender == MessageSender.OTHER) MessageSender.CHILD else MessageSender.OTHER
              )
            },
            onRemove = { messages.removeAt(index) },
          )
        }
      }
    }

    // Floating result card at top — swipe to dismiss
    AnimatedVisibility(
      visible = uiState.result != null && !resultDismissed,
      enter = slideInVertically(
        animationSpec = tween(300),
        initialOffsetY = { -it },
      ) + fadeIn(animationSpec = tween(300)),
      exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200)),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
          if (value != SwipeToDismissBoxValue.Settled) {
            resultDismissed = true
            true
          } else false
        }
      )
      SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
      ) {
        uiState.result?.let {
          SafetyResultCard(
            result = it,
            feedbackSubmitted = uiState.feedbackSubmitted,
            onFeedbackClick = { showFeedbackDialog = true },
          )
        }
      }
    }

    // Pinned bottom bar
    Surface(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
      shadowElevation = 8.dp,
      color = MaterialTheme.colorScheme.surface,
    ) {
      Column(
        modifier = Modifier
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          FilledTonalButton(
            onClick = { messages.add(ConversationMessage(MessageSender.OTHER, "")) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Other")
          }
          FilledTonalButton(
            onClick = { messages.add(ConversationMessage(MessageSender.CHILD, "")) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Child")
          }
        }
        Button(
          onClick = { onAnalyze(messages.filter { it.text.isNotBlank() }) },
          enabled = canAnalyze,
          modifier = Modifier.fillMaxWidth(),
        ) {
          if (uiState.isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          } else {
            Icon(Icons.Outlined.Shield, contentDescription = null)
          }
          Spacer(Modifier.width(8.dp))
          Text(
            when {
              modelInitializing -> "Loading model…"
              uiState.isLoading -> "Analyzing…"
              else -> "Analyze"
            }
          )
        }
      }
    }
  }

  // Feedback dialog
  if (showFeedbackDialog && !uiState.feedbackSubmitted) {
    FeedbackDialog(
      onDismiss = { showFeedbackDialog = false },
      onSubmit = { sentiment, comment ->
        onFeedback(sentiment, comment)
        showFeedbackDialog = false
      },
    )
  }
}

@Composable
private fun ChatBubbleField(
  message: ConversationMessage,
  canRemove: Boolean,
  onTextChange: (String) -> Unit,
  onSenderToggle: () -> Unit,
  onRemove: () -> Unit,
) {
  val isOther = message.sender == MessageSender.OTHER
  val bubbleColor = if (isOther)
    MaterialTheme.colorScheme.secondaryContainer
  else
    MaterialTheme.colorScheme.tertiaryContainer
  val onBubbleColor = if (isOther)
    MaterialTheme.colorScheme.onSecondaryContainer
  else
    MaterialTheme.colorScheme.onTertiaryContainer

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isOther) Alignment.Start else Alignment.End,
  ) {
    // Sender label — tap to toggle
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(bottom = 2.dp),
    ) {
      Surface(
        onClick = onSenderToggle,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
      ) {
        Text(
          text = if (isOther) "Other" else "Child",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
          color = onBubbleColor.copy(alpha = 0.7f),
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
      if (canRemove) {
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
        }
      }
    }

    // Chat bubble with text field
    Surface(
      shape = RoundedCornerShape(
        topStart = if (isOther) 4.dp else 16.dp,
        topEnd = if (isOther) 16.dp else 4.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp,
      ),
      color = bubbleColor,
      modifier = Modifier.fillMaxWidth(0.85f),
    ) {
      OutlinedTextField(
        value = message.text,
        onValueChange = onTextChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
          Text(
            if (isOther) "Type message…" else "Type reply…",
            style = MaterialTheme.typography.bodyMedium,
            color = onBubbleColor.copy(alpha = 0.5f),
          )
        },
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          imeAction = ImeAction.Default,
        ),
        maxLines = 4,
        colors = OutlinedTextFieldDefaults.colors(
          unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
          focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
          unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
          focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = onBubbleColor),
      )
    }
  }
}

@Composable
private fun FeedbackDialog(
  onDismiss: () -> Unit,
  onSubmit: (FeedbackSentiment, String) -> Unit,
) {
  var selectedSentiment by remember { mutableStateOf<FeedbackSentiment?>(null) }
  var comment by remember { mutableStateOf("") }

  val positiveColor by animateColorAsState(
    targetValue = if (selectedSentiment == FeedbackSentiment.POSITIVE)
      MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant,
    animationSpec = tween(200),
    label = "positive",
  )
  val negativeColor by animateColorAsState(
    targetValue = if (selectedSentiment == FeedbackSentiment.NEGATIVE)
      MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.surfaceVariant,
    animationSpec = tween(200),
    label = "negative",
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(8.dp),
    title = { Text("Was this result accurate?") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          FilledTonalButton(
            onClick = { selectedSentiment = FeedbackSentiment.POSITIVE },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = positiveColor),
          ) {
            Icon(Icons.Rounded.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Yes", maxLines = 1)
          }
          FilledTonalButton(
            onClick = { selectedSentiment = FeedbackSentiment.NEGATIVE },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = negativeColor),
          ) {
            Icon(Icons.Rounded.ThumbDown, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("No", maxLines = 1)
          }
        }

        OutlinedTextField(
          value = comment,
          onValueChange = { comment = it },
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("Optional comment…") },
          maxLines = 3,
        )

        Text(
          "Anonymous — includes conversation and model response.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { selectedSentiment?.let { onSubmit(it, comment) } },
        enabled = selectedSentiment != null,
      ) {
        Text("Submit")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SafetyResultCard(
  result: SafetyResult,
  feedbackSubmitted: Boolean = false,
  onFeedbackClick: () -> Unit = {},
) {
  var showDetail by remember { mutableStateOf(false) }

  if (result.parseFailed) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
          Text("Analysis Failed", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
          "Model did not return a valid result. Try a longer conversation or re-download the model.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
      }
    }
    return
  }

  val (containerColor, contentColor, icon) = when (result.riskLevel) {
    RiskLevel.NONE -> Triple(
      MaterialTheme.colorScheme.primaryContainer,
      MaterialTheme.colorScheme.onPrimaryContainer,
      Icons.Rounded.CheckCircle,
    )
    RiskLevel.LOW -> Triple(
      MaterialTheme.colorScheme.secondaryContainer,
      MaterialTheme.colorScheme.onSecondaryContainer,
      Icons.Rounded.Warning,
    )
    RiskLevel.MEDIUM -> Triple(
      MaterialTheme.colorScheme.tertiaryContainer,
      MaterialTheme.colorScheme.onTertiaryContainer,
      Icons.Rounded.Warning,
    )
    RiskLevel.HIGH, RiskLevel.CRITICAL -> Triple(
      MaterialTheme.colorScheme.errorContainer,
      MaterialTheme.colorScheme.onErrorContainer,
      Icons.Rounded.Error,
    )
  }

  val subtitle = when (result.riskLevel) {
    RiskLevel.NONE -> "No harmful content detected"
    RiskLevel.LOW -> "Mildly concerning — monitor if needed"
    RiskLevel.MEDIUM -> "Potentially harmful content found"
    RiskLevel.HIGH -> "Harmful content detected"
    RiskLevel.CRITICAL -> "Severely harmful — act immediately"
  }

  // Compact card — tap for details
  Surface(
    onClick = { showDetail = true },
    shape = RoundedCornerShape(12.dp),
    color = containerColor,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          result.riskLevel.label,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = contentColor,
        )
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.7f))
      }
      Text(
        "Details",
        style = MaterialTheme.typography.labelSmall,
        color = contentColor.copy(alpha = 0.6f),
      )
    }
  }

  // Detail dialog
  if (showDetail) {
    AlertDialog(
      onDismissRequest = { showDetail = false },
      shape = RoundedCornerShape(8.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
          Text(result.riskLevel.label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

          if (result.confidence > 0f) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Confidence", style = MaterialTheme.typography.labelMedium)
              Text("${(result.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium))
            }
            LinearProgressIndicator(
              progress = { result.confidence },
              modifier = Modifier.fillMaxWidth().height(4.dp),
              color = contentColor,
              trackColor = contentColor.copy(alpha = 0.2f),
              strokeCap = StrokeCap.Round,
            )
          }

          if (result.riskLevel == RiskLevel.NONE) {
            Text("Checked for", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              CATEGORY_LABELS.values.forEach { label ->
                Box(
                  modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                ) { Text(label, style = MaterialTheme.typography.labelSmall) }
              }
            }
          } else {
            val flagged = result.categories
              .filter { it.lowercase() != "benign" }
              .mapNotNull { CATEGORY_LABELS[it.lowercase()] ?: it.replaceFirstChar { c -> c.uppercase() }.takeIf { _ -> it.isNotBlank() } }
            if (flagged.isNotEmpty()) {
              Text("Detected categories", style = MaterialTheme.typography.labelMedium)
              FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                flagged.forEach { label ->
                  Box(
                    modifier = Modifier
                      .background(contentColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                      .padding(horizontal = 8.dp, vertical = 3.dp),
                  ) { Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor) }
                }
              }
            }
          }

          if (result.reasoning.isNotBlank()) {
            Text("Analysis", style = MaterialTheme.typography.labelMedium)
            Text(result.reasoning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f))
          }

          if (BuildConfig.DEBUG) {
            val showRaw by DebugSettings.showRawOutput.collectAsState()
            if (showRaw && result.rawResponse.isNotBlank()) {
              Text("Raw output", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
              Text(
                result.extractedJson.ifBlank { result.rawResponse },
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 10,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (!result.parseFailed) {
            TextButton(onClick = {
              showDetail = false
              onFeedbackClick()
            }) {
              Text(if (feedbackSubmitted) "Feedback sent" else "Give Feedback")
            }
          }
          TextButton(onClick = { showDetail = false }) { Text("Close") }
        }
      },
    )
  }
}
