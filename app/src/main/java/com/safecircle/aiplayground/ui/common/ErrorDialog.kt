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

package com.safecircle.aiplayground.ui.common

import com.safecircle.aiplayground.ui.common.expressive.ShapeBadge
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ErrorDialog(error: String, onDismiss: () -> Unit) {
  Dialog(onDismissRequest = onDismiss) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Badge + title
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          ShapeBadge(
            index = 1,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            size = 48.dp,
          ) {
            Icon(Icons.Rounded.Error, contentDescription = null, modifier = Modifier.size(26.dp))
          }
          Text("Error", style = MaterialTheme.typography.headlineSmallEmphasized)
        }

        // Error
        Surface(
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.errorContainer,
        ) {
          Text(
            error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Button(onClick = onDismiss) { Text("Close") }
        }
      }
    }
  }
}
