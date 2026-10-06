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

package com.safecircle.aiplayground.ui.modelmanager

import com.safecircle.aiplayground.ui.theme.heroFontFamily
import com.safecircle.aiplayground.ui.common.expressive.ShapeBadge
import com.safecircle.aiplayground.ui.common.expressive.ExpressiveCard
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.data.Model
import com.safecircle.aiplayground.data.RuntimeType
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.proto.ImportedModel
import com.safecircle.aiplayground.ui.common.TaskIcon
import com.safecircle.aiplayground.ui.common.buildTrackableUrlAnnotatedString
import com.safecircle.aiplayground.ui.common.modelitem.ModelItem
import com.safecircle.aiplayground.ui.common.tos.TosViewModel
import com.safecircle.aiplayground.ui.theme.customColors
import kotlin.text.endsWith
import kotlin.text.lowercase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "AGGlobalMM"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalModelManager(
  viewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  onModelSelected: (Task, Model) -> Unit,
  onBenchmarkClicked: (Model) -> Unit,
  modifier: Modifier = Modifier,
  tosViewModel: TosViewModel? = null,
) {
  val uiState by viewModel.uiState.collectAsState()
  val builtInModels = remember { mutableStateListOf<Model>() }
  val importedModels = remember { mutableStateListOf<Model>() }
  val taskCandidates = remember { mutableStateListOf<Task>() }
  var modelForTaskCandidate by remember { mutableStateOf<Model?>(null) }
  var showTaskSelectorBottomSheet by remember { mutableStateOf(false) }
  var showImportModelSheet by remember { mutableStateOf(false) }
  var showHuggingFaceUrlDialog by remember { mutableStateOf(false) }
  var huggingFaceUrlInput by remember { mutableStateOf("") }
  var showUnsupportedModelDialog by remember { mutableStateOf(false) }
  var unsupportedModelErrorMessage by remember { mutableStateOf("") }
  val selectedLocalModelFileUri = remember { mutableStateOf<Uri?>(null) }
  val selectedImportedModelInfo = remember { mutableStateOf<ImportedModel?>(null) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var showImportDialog by remember { mutableStateOf(false) }
  var showImportingDialog by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val modelItemExpandedStates = remember { mutableStateMapOf<String, Boolean>() }
  var selectedSection by remember { mutableStateOf<String?>(null) }

  val promoId = "vigil_banner"
  var showPromo by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    showPromo = !viewModel.dataStoreRepository.hasViewedPromo(promoId = promoId)
  }

  val filePickerLauncher: ActivityResultLauncher<Intent> =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
      if (result.resultCode == android.app.Activity.RESULT_OK) {
        result.data?.data?.let { uri ->
          validateAndProcessModelUri(
            uri = uri,
            context = context,
            isWebImport = false,
            onUnsupportedModelError = { errorMessage ->
              unsupportedModelErrorMessage = errorMessage
              showUnsupportedModelDialog = true
            },
            onValidModelUri = { validUri ->
              selectedLocalModelFileUri.value = validUri
              showImportDialog = true
            },
          )
        } ?: run { Log.d(TAG, "No file selected or URI is null.") }
      } else {
        Log.d(TAG, "File picking cancelled.")
      }
    }

  LaunchedEffect(uiState.modelImportingUpdateTrigger) {
    val allowlistModels = viewModel.allowlistModels
    val allowlistOrderMap = allowlistModels.withIndex().associate { it.value.name to it.index }

    val sortedModels =
      viewModel
        .getAllModels()
        // Filter to include only top-level models (those without a parent).
        .filter { it.parentModelName.isNullOrEmpty() }
        .sortedWith(
          compareBy<Model> { model ->
              // Sort by the index in allowlistModels. Models not in the allowlist come last.
              allowlistOrderMap[model.name] ?: Int.MAX_VALUE
            }
            .thenBy { model ->
              // If not in the allowlist, sort by their names.
              model.name
            }
        )
    builtInModels.clear()
    builtInModels.addAll(sortedModels.filter { !it.imported })
    importedModels.clear()
    importedModels.addAll(sortedModels.filter { it.imported })
  }

  // Calculate model variants by grouping models with a parentModelName.
  val modelVariants by
    remember(uiState.modelImportingUpdateTrigger) {
      derivedStateOf {
        val allModels = uiState.tasks.flatMap { it.models }.distinct()
        allModels.filter { it.parentModelName != null }.groupBy { it.parentModelName!! }
      }
    }

  val handleClickModel: (Model) -> Unit = { model ->
    val tasks = viewModel.uiState.value.tasks
    val tasksForModel = tasks.filter { task -> task.models.any { it.name == model.name } }
    // If there is only one task for the model, navigate to the model directly.
    if (tasksForModel.size == 1) {
      onModelSelected(tasksForModel[0], model)
    }
    // If there are multiple tasks for the model, show a bottom sheet for the user to choose which
    // task to use.
    else if (tasksForModel.size > 1) {
      taskCandidates.clear()
      taskCandidates.addAll(tasksForModel)
      modelForTaskCandidate = model
      showTaskSelectorBottomSheet = true
    }
  }

  // Handle system's edge swipe — back from subpage first, then close.
  BackHandler {
    if (selectedSection != null) selectedSection = null else navigateUp()
  }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = {
      LargeFlexibleTopAppBar(
        title = {
          Text(
            text = selectedSection ?: stringResource(R.string.drawer_models_label),
            fontFamily = heroFontFamily,
            fontWeight = FontWeight.ExtraBold,
          )
        },
        subtitle =
          if (selectedSection == null) {
            { Text(stringResource(R.string.drawer_models_description)) }
          } else null,
        navigationIcon = {
          if (selectedSection != null) {
            IconButton(onClick = { selectedSection = null }) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
              )
            }
          }
        },
        actions = {
          if (selectedSection == null) {
            IconButton(onClick = { navigateUp() }) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.cd_close_icon),
              )
            }
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    val modelsBySection = builtInModels
      .filter { it.parentModelName.isNullOrEmpty() }
      .groupBy { it.section ?: "" }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = innerPadding.calculateTopPadding())
    ) {
      AnimatedContent(
        targetState = selectedSection,
        transitionSpec = {
          if (targetState != null) {
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
          } else {
            slideInHorizontally { -it / 3 } togetherWith slideOutHorizontally { it }
          }
        },
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
        label = "section_nav",
      ) { section ->
        if (section == null) {
          // ── Category list ──────────────────────────────────────────────
          LazyColumn(
            modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
              top = 16.dp,
              bottom = innerPadding.calculateBottomPadding() + 80.dp,
            ),
          ) {
            item(key = "promo") {
              AnimatedVisibility(
                visible = showPromo,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }) + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
              ) {
                PromoBannerVigil(
                  onDismiss = {
                    showPromo = false
                    viewModel.dataStoreRepository.addViewedPromoId(promoId = promoId)
                  }
                )
              }
            }

            val categories = listOf(
              ModelCategory(
                name = "Horizon",
                description = "SafeCircle's production on-device models. Fine-tuned on Gemma for real-time child safety detection and on-device chat.",
                isExperimental = false,
              ),
              ModelCategory(
                name = "Horizon Edge",
                description = "Next-generation models built on Gemma 4 Edge. Higher accuracy and larger context for flagship devices.",
                isExperimental = false,
              ),
              ModelCategory(
                name = "Experimental",
                description = "Cutting-edge research and prototypes. These models may be unstable or change without notice.",
                isExperimental = true,
              ),
            )

            itemsIndexed(categories, key = { _, category -> category.name }) { index, category ->
              val count = modelsBySection[category.name]?.size ?: 0
              ModelCategoryCard(
                category = category,
                modelCount = count,
                index = index,
                onClick = { selectedSection = category.name },
              )
            }

            if (importedModels.isNotEmpty()) {
              item(key = "imported_label") {
                Text(
                  stringResource(R.string.model_list_imported_models_title),
                  color = MaterialTheme.colorScheme.onSurface,
                  style = MaterialTheme.typography.labelLarge,
                  modifier = Modifier.padding(horizontal = 4.dp).padding(top = 8.dp, bottom = 4.dp),
                )
              }
              items(importedModels, key = { "imp_${it.name}" }) { model ->
                ModelItem(
                  model = model,
                  task = null,
                  modelManagerViewModel = viewModel,
                  onModelClicked = handleClickModel,
                  onBenchmarkClicked = onBenchmarkClicked,
                  expanded = true,
                  showBenchmarkButton = model.runtimeType == RuntimeType.LITERT_LM,
                  tosViewModel = tosViewModel,
                )
              }
            }
          }
        } else {
          // ── Model subpage ──────────────────────────────────────────────
          val sectionModels = modelsBySection[section] ?: emptyList()
          val isExperimental = section == "Experimental"

          LazyColumn(
            modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
              top = 16.dp,
              bottom = innerPadding.calculateBottomPadding() + 80.dp,
            ),
          ) {
            if (isExperimental && sectionModels.isEmpty()) {
              item(key = "empty_experimental") {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                  )
                  Text(
                    "No experiments are available at the moment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            } else {
              items(sectionModels, key = { it.name }) { model ->
                val expanded = modelItemExpandedStates.getOrDefault(model.name, true)
                ModelItem(
                  model = model,
                  modelVariants = modelVariants.getOrDefault(model.name, listOf()),
                  task = null,
                  modelManagerViewModel = viewModel,
                  onModelClicked = handleClickModel,
                  onBenchmarkClicked = onBenchmarkClicked,
                  expanded = expanded,
                  showBenchmarkButton = model.runtimeType == RuntimeType.LITERT_LM,
                  onExpanded = { modelItemExpandedStates[model.name] = it },
                  tosViewModel = tosViewModel,
                )
              }
            }
          }
        }
      }

      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(alignment = Alignment.BottomCenter).padding(bottom = 32.dp),
      )
    }
  }

  if (showTaskSelectorBottomSheet) {
    ModalBottomSheet(
      onDismissRequest = { showTaskSelectorBottomSheet = false },
      sheetState = sheetState,
    ) {
      Column(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          stringResource(R.string.model_manager_select_task_title),
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(bottom = 8.dp).padding(start = 16.dp),
        )
        for (task in taskCandidates) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
              Modifier.fillMaxWidth()
                .clickable {
                  val model = modelForTaskCandidate
                  if (model != null) {
                    onModelSelected(task, model)
                  }
                  scope.launch {
                    sheetState.hide()
                    showTaskSelectorBottomSheet = false
                  }
                }
                .padding(horizontal = 16.dp, vertical = 4.dp),
          ) {
            Text(
              task.label,
              color = MaterialTheme.colorScheme.onSurface,
              style = MaterialTheme.typography.titleMedium,
            )
            TaskIcon(task = task, width = 40.dp)
          }
        }
      }
    }
  }

  // Import model bottom sheet.
  if (showImportModelSheet) {
    ModalBottomSheet(onDismissRequest = { showImportModelSheet = false }, sheetState = sheetState) {
      Text(
        "Import model",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
      )
      Text(
        stringResource(R.string.import_model_terms_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
      )
      val cbImportFromLocalFile = stringResource(R.string.cd_import_model_from_local_file_button)
      Box(
        modifier =
          Modifier.clickable {
              scope.launch {
                // Give it sometime to show the click effect.
                delay(200)
                showImportModelSheet = false

                // Show file picker.
                val intent =
                  Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    // Single select.
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                  }
                filePickerLauncher.launch(intent)
              }
            }
            .semantics {
              role = Role.Button
              contentDescription = cbImportFromLocalFile
            }
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
          Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null)
          Text("From local model file", modifier = Modifier.clearAndSetSemantics {})
        }
      }
      val cdImportFromHuggingFace = stringResource(R.string.cd_import_model_from_hugging_face)
      Box(
        modifier =
          Modifier.clickable {
              scope.launch {
                delay(200)
                showImportModelSheet = false
                showHuggingFaceUrlDialog = true
              }
            }
            .semantics {
              role = Role.Button
              contentDescription = cdImportFromHuggingFace
            }
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
          Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null)
          Text(
            stringResource(R.string.import_model_from_hugging_face),
            modifier = Modifier.clearAndSetSemantics {},
          )
        }
      }
    }
  }

  // Import dialog
  if (showImportDialog) {
    selectedLocalModelFileUri.value?.let { uri ->
      ModelImportDialog(
        uri = uri,
        onDismiss = { showImportDialog = false },
        onDone = { info ->
          selectedImportedModelInfo.value = info
          showImportDialog = false
          showImportingDialog = true
        },
      )
    }
  }

  // Importing in progress dialog.
  if (showImportingDialog) {
    selectedLocalModelFileUri.value?.let { uri ->
      selectedImportedModelInfo.value?.let { info ->
        ModelImportingDialog(
          uri = uri,
          info = info,
          onDismiss = { showImportingDialog = false },
          onDone = {
            viewModel.addImportedLlmModel(info = it)
            showImportingDialog = false

            // Show a snack bar for successful import.
            scope.launch { snackbarHostState.showSnackbar("Model imported successfully") }
          },
        )
      }
    }
  }

  // Alert dialog for unsupported model.
  if (showUnsupportedModelDialog) {
    AlertDialog(
      icon = {
        Icon(
          Icons.Rounded.Error,
          contentDescription = stringResource(R.string.cd_error),
          tint = MaterialTheme.colorScheme.error,
        )
      },
      onDismissRequest = { showUnsupportedModelDialog = false },
      title = { Text(stringResource(R.string.unsupported_model_title)) },
      text = { Text(unsupportedModelErrorMessage) },
      confirmButton = {
        Button(onClick = { showUnsupportedModelDialog = false }) {
          Text(stringResource(R.string.ok))
        }
      },
    )
  }

  if (showHuggingFaceUrlDialog) {
    AlertDialog(
      onDismissRequest = { showHuggingFaceUrlDialog = false },
      title = { Text(stringResource(R.string.import_from_hugging_face_title)) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            buildAnnotatedString {
              append(stringResource(R.string.enter_hugging_face_url))
              append(
                buildTrackableUrlAnnotatedString(
                  url = stringResource(R.string.enter_hugging_face_url_example_link),
                  linkText = stringResource(R.string.enter_hugging_face_url_example_link),
                )
              )
            }
          )
          OutlinedTextField(
            value = huggingFaceUrlInput,
            onValueChange = { huggingFaceUrlInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.hugging_face_url_placeholder)) },
            singleLine = true,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val url = huggingFaceUrlInput.trim()
            if (url.isNotEmpty()) {
              showHuggingFaceUrlDialog = false
              val uri = url.toUri()
              validateAndProcessModelUri(
                uri = uri,
                context = context,
                isWebImport = true,
                onUnsupportedModelError = { errorMessage ->
                  unsupportedModelErrorMessage = errorMessage
                  showUnsupportedModelDialog = true
                },
                onValidModelUri = { validUri ->
                  selectedLocalModelFileUri.value = validUri
                  showImportDialog = true
                },
              )
            }
          }
        ) {
          Text(stringResource(R.string.next))
        }
      },
      dismissButton = {
        TextButton(onClick = { showHuggingFaceUrlDialog = false }) {
          Text(stringResource(R.string.cancel))
        }
      },
    )
  }
}

private data class ModelCategory(
  val name: String,
  val description: String,
  val isExperimental: Boolean,
)

private val CATEGORY_BADGE_SIZE = 64.dp
private const val CATEGORY_SECONDARY_ALPHA = 0.78f

@Composable
private fun ModelCategoryCard(
  category: ModelCategory,
  modelCount: Int,
  index: Int,
  onClick: () -> Unit,
) {
  val colors = MaterialTheme.customColors
  val slot = index % colors.taskBgColors.size
  val onContainer = colors.taskOnBgColors[slot]
  if (category.isExperimental) {
    ExperimentalCategoryCard(category, modelCount, onClick)
    return
  }
  val icon =
    if (category.name == "Horizon Edge") Icons.Outlined.DevicesOther
    else Icons.AutoMirrored.Rounded.ListAlt
  ExpressiveCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    containerColor = colors.taskBgColors[slot],
    shape = MaterialTheme.shapes.extraLargeIncreased,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      ShapeBadge(
        index = index,
        containerColor = colors.taskIconColors[slot],
        contentColor = colors.taskOnIconColors[slot],
        size = CATEGORY_BADGE_SIZE,
      ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
      }
      CategoryText(category, modelCount, onContainer, Modifier.weight(1f))
      Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = onContainer)
    }
  }
}

@Composable
private fun CategoryText(
  category: ModelCategory,
  modelCount: Int,
  onContainer: Color,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        category.name,
        style = MaterialTheme.typography.titleLargeEmphasized,
        color = onContainer,
      )
      if (modelCount > 0) {
        Surface(shape = CircleShape, color = onContainer.copy(alpha = 0.14f)) {
          Text(
            "$modelCount",
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = onContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
          )
        }
      }
    }
    Text(
      category.description,
      style = MaterialTheme.typography.bodyMedium,
      color = onContainer.copy(alpha = CATEGORY_SECONDARY_ALPHA),
    )
  }
}

/** Experimental keeps its animated gradient border, restyled with the expressive shapes. */
@Composable
private fun ExperimentalCategoryCard(
  category: ModelCategory,
  modelCount: Int,
  onClick: () -> Unit,
) {
  val gradientColors = MaterialTheme.customColors.experimentalGradientColors
  val infiniteTransition = rememberInfiniteTransition(label = "exp_border")
  val angle by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
      label = "exp_border_angle",
    )
  val onSurface = MaterialTheme.colorScheme.onSurface
  val outer = MaterialTheme.shapes.extraLargeIncreased
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = outer,
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .drawBehind {
            val angleRad = Math.toRadians(angle.toDouble())
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = kotlin.math.sqrt(cx * cx + cy * cy)
            drawRect(
              brush =
                Brush.linearGradient(
                  colors = gradientColors,
                  start =
                    Offset(
                      cx + (-r * kotlin.math.cos(angleRad)).toFloat(),
                      cy + (-r * kotlin.math.sin(angleRad)).toFloat(),
                    ),
                  end =
                    Offset(
                      cx + (r * kotlin.math.cos(angleRad)).toFloat(),
                      cy + (r * kotlin.math.sin(angleRad)).toFloat(),
                    ),
                ),
              size = size,
            )
          }
          .padding(3.dp)
          .clip(MaterialTheme.shapes.extraLarge)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .padding(20.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Box(
          modifier =
            Modifier.size(CATEGORY_BADGE_SIZE)
              .clip(MaterialShapes.SoftBurst.toShape())
              .background(Brush.linearGradient(gradientColors.take(3))),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp),
          )
        }
        CategoryText(category, modelCount, onSurface, Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = onSurface)
      }
    }
  }
}

private fun validateAndProcessModelUri(
  uri: Uri,
  context: Context,
  isWebImport: Boolean,
  onUnsupportedModelError: (String) -> Unit,
  onValidModelUri: (Uri) -> Unit,
) {
  val fileName = getFileName(context = context, uri = uri)
  Log.d(TAG, "Validating URI: $uri, fileName: $fileName, isWebImport: $isWebImport")
  val hasValidExtension =
    if (isWebImport) {
      fileName != null && fileName.endsWith(".litertlm")
    } else {
      fileName != null && (fileName.endsWith(".task") || fileName.endsWith(".litertlm"))
    }

  if (!hasValidExtension) {
    onUnsupportedModelError(getErrorMessage(context, R.string.unsupported_file_type_error))
  } else if (fileName != null && fileName.lowercase().contains("-web")) {
    onUnsupportedModelError(getErrorMessage(context, R.string.unsupported_web_model_error))
  } else {
    onValidModelUri(uri)
  }
}

private fun getErrorMessage(context: Context, resId: Int): String {
  return context.getString(resId)
}

// Helper function to get the file name from a URI
private fun getFileName(context: Context, uri: Uri): String? {
  if (uri.scheme == "content") {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1) {
          return cursor.getString(nameIndex)
        }
      }
    }
  } else if (uri.scheme == "file" || uri.scheme == "http" || uri.scheme == "https") {
    return uri.lastPathSegment
  }
  return null
}
