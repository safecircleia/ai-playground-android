package com.safecircle.aiplayground.customtasks.agentchat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.proto.McpAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpManagerBottomSheet(
  mcpManagerViewModel: McpManagerViewModel,
  onDismiss: (selectedChanged: Boolean) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val uiState by mcpManagerViewModel.uiState.collectAsState()
  var changed by remember { mutableStateOf(false) }
  var newServerUrl by remember { mutableStateOf("") }

  ModalBottomSheet(onDismissRequest = { onDismiss(changed) }, sheetState = sheetState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
      Text("MCP Servers", style = MaterialTheme.typography.titleLarge)
      Spacer(modifier = Modifier.height(12.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = newServerUrl,
          onValueChange = { newServerUrl = it },
          modifier = Modifier.weight(1f),
          placeholder = { Text("https://mcp-server-url") },
          singleLine = true,
        )
        Button(
          onClick = {
            if (newServerUrl.isNotBlank()) {
              mcpManagerViewModel.addMcpServer(newServerUrl.trim(), McpAuth.AuthMethodCase.NONE, "", "")
              newServerUrl = ""
              changed = true
            }
          },
          enabled = !uiState.loadingMcpServer,
        ) { Text("Add") }
      }

      if (uiState.error != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()

      if (uiState.mcpServers.isEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("No MCP servers configured", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
          items(uiState.mcpServers) { serverState ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(serverState.mcpServer.name.ifEmpty { serverState.mcpServer.url }, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                  if (serverState.error != null) {
                    Text(serverState.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                  } else {
                    Text("${serverState.mcpServer.toolsList.count { it.enabled }} tools enabled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
                Switch(
                  checked = serverState.mcpServer.enabled,
                  onCheckedChange = { enabled -> mcpManagerViewModel.setMcpServerEnabled(serverState.mcpServer.url, enabled); changed = true },
                )
              }
              if (serverState.mcpServer.enabled && serverState.mcpServer.toolsList.isNotEmpty()) {
                serverState.mcpServer.toolsList.forEach { tool ->
                  Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(tool.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(
                      checked = tool.enabled,
                      onCheckedChange = { enabled -> mcpManagerViewModel.setMcpToolEnabled(serverState.mcpServer.url, tool.name, enabled); changed = true },
                    )
                  }
                }
              }
              TextButton(onClick = { mcpManagerViewModel.removeMcpServer(serverState.mcpServer.url); changed = true }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            }
            HorizontalDivider()
          }
        }
      }
    }
  }
}
