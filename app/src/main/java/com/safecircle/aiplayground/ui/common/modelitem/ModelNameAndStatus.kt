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

package com.safecircle.aiplayground.ui.common.modelitem

import androidx.compose.ui.text.font.FontWeight
import com.safecircle.aiplayground.logEvent
import com.safecircle.aiplayground.GalleryEvent
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.data.MODEL_INFO_ICON_SIZE
import com.safecircle.aiplayground.data.Model
import com.safecircle.aiplayground.data.ModelDownloadStatus
import com.safecircle.aiplayground.data.ModelDownloadStatusType
import com.safecircle.aiplayground.data.RuntimeType
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.ui.common.ClickableLink
import com.safecircle.aiplayground.ui.common.humanReadableSize
import com.safecircle.aiplayground.ui.theme.customColors
import com.safecircle.aiplayground.ui.theme.labelSmallNarrow

/**
 * Composable function to display the model name and its download status information.
 *
 * This function renders the model's name and its current download status, including:
 * - Model name.
 * - Failure message (if download failed).
 * - "Unzipping..." status for unzipping processes.
 * - Model size for successful downloads.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
fun ModelNameAndStatus(
  model: Model,
  task: Task?,
  downloadStatus: ModelDownloadStatus?,
  isExpanded: Boolean,
  modifier: Modifier = Modifier,
  showModelSizeAndDownloadProgressLabel: Boolean = true,
) {
  var showUpdateDialog by remember { mutableStateOf(false) }

  Column(modifier = modifier) {
    // Show "best overall" only for the first model if it is indeed the best for this task.
    if (task != null && model.bestForTaskIds.contains(task.id) && task.models[0] == model) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 6.dp),
      ) {
        Icon(
          Icons.Filled.Star,
          tint = MaterialTheme.customColors.warningTextColor,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Text(
          stringResource(R.string.best_overall),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.alpha(0.6f),
        )
      }
    }

    // Show "Update available" info message label if the model is updatable.
    // Tap to show the detailed update info in a dialog.
    if (model.updatable) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
          Modifier.padding(bottom = 10.dp)
            .then(
              if (model.updateInfo.isNotEmpty()) {
                Modifier.clickable { showUpdateDialog = true }
              } else {
                Modifier
              }
            ),
      ) {
        Icon(
          Icons.Filled.Info,
          tint = MaterialTheme.colorScheme.primary,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Text(
          stringResource(R.string.update_available),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (showUpdateDialog) {
      AlertDialog(
        onDismissRequest = { showUpdateDialog = false },
        title = { Text(stringResource(R.string.about_this_update)) },
        text = { Text(model.updateInfo) },
        confirmButton = {
          TextButton(onClick = { showUpdateDialog = false }) {
            Text(stringResource(android.R.string.ok))
          }
        },
      )
    }

    // Model name.
    Text(
      model.displayName.ifEmpty { model.name },
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      style = MaterialTheme.typography.titleLargeEmphasized,
      fontWeight = FontWeight.SemiBold,
    )

    // Info chips: version, size / download status, and a link to the model license.
    val status = downloadStatus?.status
    val statusActive =
      status == ModelDownloadStatusType.IN_PROGRESS ||
        status == ModelDownloadStatusType.PARTIALLY_DOWNLOADED ||
        status == ModelDownloadStatusType.UNZIPPING
    FlowRow(
      modifier = Modifier.padding(top = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (model.version.isNotEmpty()) {
        val versionText =
          if (model.updatable && model.latestModelFile != null) {
            "v${model.version} → v${model.latestModelFile!!.commitHash} available"
          } else {
            "v${model.version}"
          }
        InfoChip(
          containerColor =
            if (model.updatable) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.secondaryContainer
        ) {
          Text(
            versionText,
            style = MaterialTheme.typography.labelLargeEmphasized,
            color =
              if (model.updatable) MaterialTheme.colorScheme.onPrimaryContainer
              else MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
      }

      if (model.runtimeType != RuntimeType.AICORE && showModelSizeAndDownloadProgressLabel) {
        val failed = status == ModelDownloadStatusType.FAILED
        InfoChip(
          containerColor =
            when {
              failed -> MaterialTheme.colorScheme.errorContainer
              statusActive -> MaterialTheme.colorScheme.primaryContainer
              else -> MaterialTheme.colorScheme.secondaryContainer
            }
        ) {
          ModelStatusDetails(
            model = model,
            task = task,
            downloadStatus = downloadStatus,
            isExpanded = false,
            contentColor =
              if (statusActive) MaterialTheme.colorScheme.onPrimaryContainer
              else MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
      }

      if (!model.imported && model.learnMoreUrl.isNotEmpty()) {
        LearnMoreChip(model.learnMoreUrl)
      }
    }
  }
}

@Composable
private fun InfoChip(containerColor: Color, content: @Composable () -> Unit) {
  Surface(shape = CircleShape, color = containerColor) {
    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) { content() }
  }
}

@Composable
private fun LearnMoreChip(url: String) {
  val uriHandler = LocalUriHandler.current
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.tertiaryContainer,
    onClick = {
      uriHandler.openUri(url)
      logEvent(
        GalleryEvent.BUTTON_CLICKED,
        mapOf("event_type" to "resource_link_click", "link_destination" to url),
      )
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        Icons.AutoMirrored.Outlined.OpenInNew,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.size(MODEL_INFO_ICON_SIZE),
      )
      Text(
        stringResource(R.string.model_license_chip_label),
        style = MaterialTheme.typography.labelLargeEmphasized,
        maxLines = 1,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
    }
  }
}

@Composable
fun ModelStatusDetails(
  model: Model,
  task: Task?,
  downloadStatus: ModelDownloadStatus?,
  isExpanded: Boolean,
  modifier: Modifier = Modifier,
  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
  val inProgress = downloadStatus?.status == ModelDownloadStatusType.IN_PROGRESS
  val isPartiallyDownloaded = downloadStatus?.status == ModelDownloadStatusType.PARTIALLY_DOWNLOADED
  var curDownloadProgress = 0f

  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
    // Status icon.
    StatusIcon(
      task = task,
      model = model,
      downloadStatus = downloadStatus,
      modifier = Modifier.padding(end = 4.dp),
    )

    // Failure message.
    if (downloadStatus != null && downloadStatus.status == ModelDownloadStatusType.FAILED) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          downloadStatus.errorMessage,
          color = MaterialTheme.colorScheme.error,
          style = labelSmallNarrow,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    // Status label
    else {
      var sizeLabel = model.totalBytes.humanReadableSize()
      if (model.localFileRelativeDirPathOverride.isNotEmpty()) {
        sizeLabel = "{ext_files_dir}/${model.localFileRelativeDirPathOverride}"
      }

      // Populate the status label.
      if (downloadStatus != null) {
        // For in-progress model, show {receivedSize} / {totalSize} - {rate} - {remainingTime}
        if (inProgress || isPartiallyDownloaded) {
          var totalSize = downloadStatus.totalBytes
          if (totalSize == 0L) {
            totalSize = model.totalBytes
          }
          sizeLabel =
            "${downloadStatus.receivedBytes.humanReadableSize(extraDecimalForGbAndAbove = true)} of ${totalSize.humanReadableSize()}"
          if (downloadStatus.bytesPerSecond > 0) {
            sizeLabel = "$sizeLabel · ${downloadStatus.bytesPerSecond.humanReadableSize()} / s"
            // if (downloadStatus.remainingMs >= 0) {
            //   sizeLabel =
            //     "$sizeLabel\n${downloadStatus.remainingMs.formatToHourMinSecond()} left"
            // }
          }
          if (isPartiallyDownloaded) {
            sizeLabel = "$sizeLabel (resuming...)"
          }
          curDownloadProgress =
            downloadStatus.receivedBytes.toFloat() / downloadStatus.totalBytes.toFloat()
          if (curDownloadProgress.isNaN()) {
            curDownloadProgress = 0f
          }
        }
        // Status for unzipping.
        else if (downloadStatus.status == ModelDownloadStatusType.UNZIPPING) {
          sizeLabel = "Unzipping..."
        }
      }

      Column(
        horizontalAlignment = if (isExpanded) Alignment.CenterHorizontally else Alignment.Start
      ) {
        for ((index, line) in sizeLabel.split("\n").withIndex()) {
          Text(
            line,
            color = contentColor,
            maxLines = 1,
            style =
              MaterialTheme.typography.bodyMedium.copy(
                // This stops numbers from "jumping around" when being updated.
                fontFeatureSettings = "tnum"
              ),
            overflow = TextOverflow.Visible,
            modifier = Modifier.offset(y = if (index == 0) 0.dp else (-1).dp),
          )
        }
      }
    }
  }
}
