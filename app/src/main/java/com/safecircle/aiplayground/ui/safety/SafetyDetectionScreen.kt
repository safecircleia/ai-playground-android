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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.ui.modelmanager.ModelManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyDetectionScreen(
  modelManagerViewModel: ModelManagerViewModel,
  viewModel: SafetyDetectionViewModel,
  navigateUp: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  var inputText by remember { mutableStateOf("") }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("Safety Detection") },
        navigationIcon = {
          IconButton(onClick = navigateUp) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp)
          .imePadding()
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      Text(
        "Analyze text for potential safety risks using an on-device AI model.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      OutlinedTextField(
        value = inputText,
        onValueChange = { inputText = it },
        modifier = Modifier.fillMaxWidth().height(160.dp),
        label = { Text("Enter text to analyze") },
        placeholder = { Text("Type or paste any text here…") },
        keyboardOptions =
          KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default,
          ),
        maxLines = 8,
      )

      Button(
        onClick = {
          viewModel.analyze(
            modelManagerViewModel = modelManagerViewModel,
            text = inputText,
          )
        },
        enabled = inputText.isNotBlank() && !uiState.isLoading,
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
        Spacer(modifier = Modifier.size(8.dp))
        Text(if (uiState.isLoading) "Analyzing…" else "Analyze")
      }

      AnimatedVisibility(visible = uiState.result != null, enter = fadeIn(), exit = fadeOut()) {
        uiState.result?.let { result -> SafetyResultCard(result) }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun SafetyResultCard(result: SafetyResult) {
  val isSafe = result.riskLevel == RiskLevel.SAFE
  val containerColor =
    if (isSafe) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.errorContainer
  val contentColor =
    if (isSafe) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onErrorContainer

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = containerColor,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
          if (isSafe) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(28.dp),
        )
        Text(
          text = result.riskLevel.label,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = contentColor,
        )
      }

      if (result.categories.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          result.categories.forEach { category ->
            Box(
              modifier =
                Modifier.background(
                  color = contentColor.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(category, style = MaterialTheme.typography.labelMedium, color = contentColor)
            }
          }
        }
      }

      if (result.explanation.isNotBlank()) {
        Text(
          result.explanation,
          style = MaterialTheme.typography.bodySmall,
          color = contentColor.copy(alpha = 0.8f),
        )
      }
    }
  }
}
