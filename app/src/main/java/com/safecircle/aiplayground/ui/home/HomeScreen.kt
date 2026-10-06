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

package com.safecircle.aiplayground.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.data.CategoryInfo
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.ui.common.tos.AppTosDialog
import com.safecircle.aiplayground.ui.common.tos.TosViewModel
import com.safecircle.aiplayground.ui.modelmanager.ModelManagerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LOADING_DEBOUNCE_MS = 200L
private const val NOTIFICATION_PERMISSION_DELAY_MS = 2000L
private const val DRAWER_NAV_DELAY_MS = 50L

@Composable
fun HomeScreen(
  modelManagerViewModel: ModelManagerViewModel,
  tosViewModel: TosViewModel,
  navigateToTaskScreen: (Task) -> Unit,
  onModelsClicked: () -> Unit,
  enableAnimation: Boolean,
  modifier: Modifier = Modifier,
  layout: HomeLayout = HomeLayout.COMPACT,
  detailPane: (@Composable () -> Unit)? = null,
) {
  val uiState by modelManagerViewModel.uiState.collectAsState()
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showTosDialog by remember { mutableStateOf(!tosViewModel.getIsTosAccepted()) }
  val context = LocalContext.current

  val sortedCategories =
    remember(uiState.tasks) {
      sortCategories(uiState.tasks.map { it.category }.distinctBy { it.id }) {
        getCategoryLabel(context, it)
      }
    }

  if (!showTosDialog) {
    // Only show the spinner if loading takes longer than the debounce, to avoid a flicker.
    val showSpinner = rememberDebouncedLoading(uiState.loadingModelAllowlist)
    when {
      showSpinner -> HomeLoading()
      !uiState.loadingModelAllowlist ->
        HomeContent(
          sortedCategories = sortedCategories,
          tasksByCategory = uiState.tasksByCategory,
          enableAnimation = enableAnimation,
          navigateToTaskScreen = navigateToTaskScreen,
          onModelsClicked = onModelsClicked,
          onSettingsClicked = { showSettingsDialog = true },
          layout = layout,
          detailPane = detailPane,
          modifier = modifier,
        )
    }
  }

  if (showTosDialog) {
    AppTosDialog(
      onTosAccepted = {
        showTosDialog = false
        tosViewModel.acceptTos()
      }
    )
  }

  if (showSettingsDialog) {
    SettingsDialog(
      curThemeOverride = modelManagerViewModel.readThemeOverride(),
      modelManagerViewModel = modelManagerViewModel,
      onDismissed = { showSettingsDialog = false },
    )
  }

  if (uiState.loadingModelAllowlistError.isNotEmpty()) {
    AlertDialog(
      icon = {
        Icon(
          Icons.Rounded.Error,
          contentDescription = stringResource(R.string.cd_error),
          tint = MaterialTheme.colorScheme.error,
        )
      },
      title = { Text(uiState.loadingModelAllowlistError) },
      text = { Text("Please check your internet connection and try again later.") },
      onDismissRequest = { modelManagerViewModel.loadModelAllowlist() },
      confirmButton = {
        TextButton(onClick = { modelManagerViewModel.loadModelAllowlist() }) { Text("Retry") }
      },
      dismissButton = {
        TextButton(onClick = { modelManagerViewModel.clearLoadModelAllowlistError() }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun HomeContent(
  sortedCategories: List<CategoryInfo>,
  tasksByCategory: Map<String, List<Task>>,
  enableAnimation: Boolean,
  navigateToTaskScreen: (Task) -> Unit,
  onModelsClicked: () -> Unit,
  onSettingsClicked: () -> Unit,
  layout: HomeLayout,
  detailPane: (@Composable () -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  NotificationPermissionEffect()

  val scaffold: @Composable (onMenuClick: (() -> Unit)?, sharedBadges: Boolean, Modifier) -> Unit =
    { onMenuClick, sharedBadges, scaffoldModifier ->
      HomeScaffold(
        sortedCategories = sortedCategories,
        tasksByCategory = tasksByCategory,
        enableAnimation = enableAnimation,
        sharedBadges = sharedBadges,
        onTaskClick = navigateToTaskScreen,
        onMenuClick = onMenuClick,
        modifier = scaffoldModifier,
      )
    }

  if (layout == HomeLayout.COMPACT) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // Close the drawer when back is pressed.
    BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }
    ModalNavigationDrawer(
      drawerState = drawerState,
      drawerContent = {
        HomeDrawerContent(
          onModelsClick = {
            scope.launch {
              drawerState.close()
              delay(DRAWER_NAV_DELAY_MS)
              onModelsClicked()
            }
          },
          onSettingsClick = {
            onSettingsClicked()
            scope.launch { drawerState.close() }
          },
        )
      },
      gesturesEnabled = drawerState.isOpen,
      modifier = modifier,
    ) {
      scaffold(
        {
          scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() }
        },
        true,
        Modifier,
      )
    }
  } else {
    Row(modifier = modifier.fillMaxSize()) {
      HomeRail(onModelsClick = onModelsClicked, onSettingsClick = onSettingsClicked)
      if (layout == HomeLayout.EXPANDED && detailPane != null) {
        // Both panes are on screen, so the shared-element key would be used twice: disable it.
        HomeListDetail(
          list = { scaffold(null, false, Modifier) },
          detail = detailPane,
          modifier = Modifier.weight(1f),
        )
      } else {
        scaffold(null, true, Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun rememberDebouncedLoading(loading: Boolean): Boolean {
  var delayed by remember { mutableStateOf(false) }
  LaunchedEffect(loading) {
    if (loading) {
      delay(LOADING_DEBOUNCE_MS)
      delayed = true
    } else {
      delayed = false
    }
  }
  return delayed
}

@Composable
private fun HomeLoading() {
  Row(
    modifier = Modifier.fillMaxSize(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    LoadingIndicator(modifier = Modifier.padding(end = 12.dp))
    Text(stringResource(R.string.loading_model_list), style = MaterialTheme.typography.bodyMedium)
  }
}

/** Asks for the notification permission shortly after the home screen is shown (API 33+). */
@Composable
private fun NotificationPermissionEffect() {
  val context = LocalContext.current
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
  LaunchedEffect(Unit) {
    delay(NOTIFICATION_PERMISSION_DELAY_MS)
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
}
