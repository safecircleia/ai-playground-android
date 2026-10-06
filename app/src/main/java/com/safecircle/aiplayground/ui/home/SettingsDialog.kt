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

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.safecircle.aiplayground.BuildConfig
import com.safecircle.aiplayground.GalleryEvent
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.data.DebugSettings
import com.safecircle.aiplayground.logEvent
import com.safecircle.aiplayground.proto.Theme
import com.safecircle.aiplayground.ui.common.expressive.ShapeBadge
import com.safecircle.aiplayground.ui.common.tos.AppTosDialog
import com.safecircle.aiplayground.ui.modelmanager.ModelManagerViewModel
import com.safecircle.aiplayground.ui.theme.ThemeSettings
import com.safecircle.aiplayground.ui.theme.heroFontFamily

private val THEME_OPTIONS =
  listOf(Theme.THEME_AUTO, Theme.THEME_LIGHT, Theme.THEME_DARK, Theme.THEME_AMOLED)
private const val MODEL_LICENSE_URL = "https://safecircle.tech/licenses/research"
private val ROW_ICON_SIZE = 40.dp

/** App settings, shown as a bottom sheet with grouped sections. */
@Composable
fun SettingsDialog(
  curThemeOverride: Theme,
  modelManagerViewModel: ModelManagerViewModel,
  onDismissed: () -> Unit,
) {
  var selectedTheme by remember { mutableStateOf(curThemeOverride) }
  var showTos by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val uriHandler = LocalUriHandler.current
  val showRawOutput by DebugSettings.showRawOutput.collectAsState()

  ModalBottomSheet(
    onDismissRequest = onDismissed,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      SettingsHeader()

      SettingsSection("Appearance") {
        Column(
          modifier =
            Modifier.fillMaxWidth().padding(16.dp).semantics(mergeDescendants = true) {},
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Theme", style = MaterialTheme.typography.titleMedium)
          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            THEME_OPTIONS.forEachIndexed { index, theme ->
              SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = THEME_OPTIONS.size),
                onClick = {
                  selectedTheme = theme
                  ThemeSettings.themeOverride.value = theme
                  modelManagerViewModel.saveThemeOverride(theme)
                  val uiModeManager =
                    context.applicationContext.getSystemService(Context.UI_MODE_SERVICE)
                      as UiModeManager
                  when (theme) {
                    Theme.THEME_AUTO ->
                      uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO)
                    Theme.THEME_LIGHT ->
                      uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
                    else -> uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
                  }
                },
                selected = theme == selectedTheme,
                label = {
                  Text(themeLabel(theme), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                },
              )
            }
          }
        }
      }

      SettingsSection("About & legal") {
        SettingsRow(
          icon = Icons.Rounded.Code,
          title = "Third-party libraries",
          subtitle = "View licenses",
          trailing = Icons.Rounded.ChevronRight,
          onClick = {
            // Launch a license viewer listing third-party library names; tapping one shows its
            // license content.
            context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
          },
        )
        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
        SettingsRow(
          icon = Icons.Rounded.Description,
          title = stringResource(R.string.settings_dialog_tos_title),
          subtitle = stringResource(R.string.settings_dialog_view_app_terms_of_service),
          trailing = Icons.Rounded.ChevronRight,
          onClick = { showTos = true },
        )
        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
        SettingsRow(
          icon = Icons.Rounded.Gavel,
          title = stringResource(R.string.tos_dialog_title_gemma),
          subtitle = "safecircle.tech",
          trailing = Icons.AutoMirrored.Rounded.OpenInNew,
          onClick = {
            uriHandler.openUri(MODEL_LICENSE_URL)
            logEvent(
              GalleryEvent.BUTTON_CLICKED,
              mapOf("event_type" to "resource_link_click", "link_destination" to MODEL_LICENSE_URL),
            )
          },
        )
      }

      // Debug options — only visible in debug builds.
      if (BuildConfig.DEBUG) {
        SettingsSection("Developer") {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            RowIcon(Icons.Rounded.BugReport)
            Column(modifier = Modifier.weight(1f)) {
              Text("Show raw model output", style = MaterialTheme.typography.titleMedium)
              Text(
                "Safety Detection: show full JSON response",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = showRawOutput,
              onCheckedChange = { DebugSettings.setShowRawOutput(context, it) },
            )
          }
        }
      }
    }
  }

  if (showTos) {
    AppTosDialog(onTosAccepted = { showTos = false }, viewingMode = true)
  }
}

@Composable
private fun SettingsHeader() {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    ShapeBadge(
      index = 0,
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      size = 56.dp,
    ) {
      Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(28.dp))
    }
    Column {
      Text(
        "Settings",
        style = MaterialTheme.typography.headlineMediumEmphasized,
        fontFamily = heroFontFamily,
        fontWeight = FontWeight.ExtraBold,
      )
      Text(
        "App version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** A titled group of settings drawn as one tonal container. */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      title,
      style = MaterialTheme.typography.labelLargeEmphasized,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(horizontal = 8.dp),
    )
    Surface(
      shape = MaterialTheme.shapes.extraLargeIncreased,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Column(content = content)
    }
  }
}

@Composable
private fun SettingsRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  trailing: ImageVector,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    RowIcon(icon)
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Icon(trailing, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun RowIcon(icon: ImageVector) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.secondaryContainer,
    modifier = Modifier.size(ROW_ICON_SIZE),
  ) {
    Icon(
      icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSecondaryContainer,
      modifier = Modifier.padding(10.dp),
    )
  }
}

private fun themeLabel(theme: Theme): String {
  return when (theme) {
    Theme.THEME_AUTO -> "Auto"
    Theme.THEME_LIGHT -> "Light"
    Theme.THEME_DARK -> "Dark"
    Theme.THEME_AMOLED -> "Black"
    else -> "Unknown"
  }
}
