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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillManagerBottomSheet(
  agentTools: AgentTools,
  skillManagerViewModel: SkillManagerViewModel,
  onDismiss: (selectedSkillsChanged: Boolean) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val uiState by skillManagerViewModel.uiState.collectAsState()
  var changed by remember { mutableStateOf(false) }

  ModalBottomSheet(onDismissRequest = { onDismiss(changed) }, sheetState = sheetState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Skills", style = MaterialTheme.typography.titleLarge)
        Row {
          TextButton(onClick = { skillManagerViewModel.setAllSkillsSelected(true); changed = true }) { Text("All") }
          TextButton(onClick = { skillManagerViewModel.setAllSkillsSelected(false); changed = true }) { Text("None") }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider()
      LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
        items(uiState.skills) { skillState ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(skillState.skill.name, style = MaterialTheme.typography.bodyLarge)
              Text(skillState.skill.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Switch(
              checked = skillState.skill.selected,
              onCheckedChange = { selected -> skillManagerViewModel.setSkillSelected(skillState, selected); changed = true },
            )
          }
        }
      }
    }
  }
}
