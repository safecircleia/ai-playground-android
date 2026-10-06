package com.safecircle.aiplayground.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.R

private val LIST_PANE_WIDTH = 420.dp

/** Two-pane home: use cases on the left, the selected use case's models on the right. */
@Composable
fun HomeListDetail(
  list: @Composable () -> Unit,
  detail: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  // The navigator supplies the window-aware directive/value (hinges, partitions).
  val navigator = rememberListDetailPaneScaffoldNavigator<Unit>()
  ListDetailPaneScaffold(
    directive = navigator.scaffoldDirective,
    value = navigator.scaffoldValue,
    modifier = modifier,
    listPane = { AnimatedPane(Modifier.preferredWidth(LIST_PANE_WIDTH)) { list() } },
    detailPane = { AnimatedPane { detail() } },
  )
}

/** Shown in the detail pane until a use case is selected. */
@Composable
fun HomeDetailPlaceholder() {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(
      stringResource(R.string.home_select_task_hint),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
