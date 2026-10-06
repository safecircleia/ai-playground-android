package com.safecircle.aiplayground.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.data.CategoryInfo
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.ui.common.TaskIcon
import com.safecircle.aiplayground.ui.common.expressive.ExpressiveCard
import com.safecircle.aiplayground.ui.common.expressive.sharedBadge
import com.safecircle.aiplayground.ui.common.expressive.staggeredEntrance
import com.safecircle.aiplayground.ui.theme.customColors

/** One page of task cards per category. */
@Composable
fun TaskList(
  pagerState: PagerState,
  sortedCategories: List<CategoryInfo>,
  tasksByCategory: Map<String, List<Task>>,
  enableAnimation: Boolean,
  sharedBadges: Boolean,
  onTaskClick: (Task) -> Unit,
) {
  HorizontalPager(
    state = pagerState,
    verticalAlignment = Alignment.Top,
    contentPadding = PaddingValues(horizontal = 16.dp),
    pageSpacing = 16.dp,
  ) { page ->
    val tasks = tasksByCategory[sortedCategories[page].id].orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      tasks.forEachIndexed { index, task ->
        TaskCard(
          task = task,
          onClick = { onTaskClick(task) },
          sharedBadge = sharedBadges,
          modifier =
            Modifier.fillMaxWidth().staggeredEntrance(index, enabled = enableAnimation && page == 0),
        )
      }
    }
  }
}

@Composable
fun TaskCard(
  task: Task,
  onClick: () -> Unit,
  sharedBadge: Boolean,
  modifier: Modifier = Modifier,
) {
  // Reading updateTrigger makes the count recompose when models are added/removed.
  val modelCount by remember {
    derivedStateOf { if (task.updateTrigger.value >= 0) task.models.size else 0 }
  }
  val cardDescription = stringResource(R.string.cd_task_card, task.label, modelCount)
  ExpressiveCard(
    onClick = onClick,
    modifier = modifier.semantics { contentDescription = cardDescription },
    containerColor = MaterialTheme.customColors.taskCardBgColor,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      TaskIcon(
        task = task,
        modifier = Modifier.sharedBadge(task.id, enabled = sharedBadge),
        width = 56.dp,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            task.label,
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (task.newFeature) NewBadge()
          if (task.experimental) {
            Icon(
              painter = painterResource(R.drawable.ic_experiment),
              contentDescription = "Experimental",
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        ModelCountLabel(modelCount)
        if (task.shortDescription.isNotEmpty()) {
          Text(
            task.shortDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics {},
          )
        }
      }
    }
  }
}

@Composable
private fun NewBadge() {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.customColors.newFeatureContainerColor,
  ) {
    Text(
      "New",
      color = MaterialTheme.customColors.newFeatureTextColor,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    )
  }
}

@Composable
private fun ModelCountLabel(count: Int) {
  val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
  AnimatedContent(
    targetState = count,
    transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
    label = "model count",
  ) { value ->
    Text(
      if (value == 1) "1 Model" else "$value Models",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.clearAndSetSemantics {},
    )
  }
}
