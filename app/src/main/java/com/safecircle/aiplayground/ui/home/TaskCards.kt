package com.safecircle.aiplayground.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.data.CategoryInfo
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.ui.common.TaskIcon
import com.safecircle.aiplayground.ui.common.expressive.ExpressiveCard
import com.safecircle.aiplayground.ui.common.expressive.sharedBadge
import com.safecircle.aiplayground.ui.common.expressive.staggeredEntrance
import com.safecircle.aiplayground.ui.common.getTaskBgColor
import com.safecircle.aiplayground.ui.common.getTaskOnBgColor
import com.safecircle.aiplayground.ui.theme.customColors
import com.safecircle.aiplayground.ui.theme.heroFontFamily

private val CARD_GAP = 12.dp
private val HERO_BADGE_SIZE = 72.dp
private val TILE_BADGE_SIZE = 48.dp
private val HERO_DECOR_SIZE = 220.dp
private const val HERO_DECOR_ALPHA = 0.1f
private const val SECONDARY_TEXT_ALPHA = 0.78f
private const val PILL_ALPHA = 0.14f

/**
 * One page of tasks per category: the first task is the hero (size + colour make it the obvious
 * start), the rest sit in a two-column grid of coloured tiles.
 */
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
    val animate = enableAnimation && page == 0
    Column(verticalArrangement = Arrangement.spacedBy(CARD_GAP)) {
      tasks.firstOrNull()?.let { hero ->
        HeroTaskCard(
          task = hero,
          onClick = { onTaskClick(hero) },
          sharedBadge = sharedBadges,
          modifier = Modifier.fillMaxWidth().staggeredEntrance(0, animate),
        )
      }
      tasks.drop(1).chunked(2).forEachIndexed { row, pair ->
        Row(
          modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
          horizontalArrangement = Arrangement.spacedBy(CARD_GAP),
        ) {
          pair.forEachIndexed { column, task ->
            TaskTile(
              task = task,
              onClick = { onTaskClick(task) },
              sharedBadge = sharedBadges,
              modifier =
                Modifier.weight(1f)
                  .fillMaxHeight()
                  .staggeredEntrance(1 + row * 2 + column, animate),
            )
          }
          if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun rememberModelCount(task: Task): Int {
  // Reading updateTrigger makes the count recompose when models are added/removed.
  val count by remember {
    derivedStateOf { if (task.updateTrigger.value >= 0) task.models.size else 0 }
  }
  return count
}

@Composable
private fun HeroTaskCard(
  task: Task,
  onClick: () -> Unit,
  sharedBadge: Boolean,
  modifier: Modifier = Modifier,
) {
  val modelCount = rememberModelCount(task)
  val onContainer = getTaskOnBgColor(task)
  val cardDescription = stringResource(R.string.cd_task_card, task.label, modelCount)
  ExpressiveCard(
    onClick = onClick,
    modifier = modifier.semantics { contentDescription = cardDescription },
    containerColor = getTaskBgColor(task),
    shape = MaterialTheme.shapes.extraExtraLarge,
  ) {
    Box {
      // Decorative expressive shape bleeding off the corner.
      Box(
        modifier =
          Modifier.align(Alignment.TopEnd)
            .offset(x = 72.dp, y = (-72).dp)
            .size(HERO_DECOR_SIZE)
            .clip(MaterialShapes.Cookie12Sided.toShape())
            .background(onContainer.copy(alpha = HERO_DECOR_ALPHA))
      )
      Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        TaskIcon(
          task = task,
          modifier = Modifier.sharedBadge(task.id, enabled = sharedBadge),
          width = HERO_BADGE_SIZE,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            task.label,
            style = MaterialTheme.typography.headlineLargeEmphasized,
            fontFamily = heroFontFamily,
            fontWeight = FontWeight.ExtraBold,
            color = onContainer,
          )
          if (task.shortDescription.isNotEmpty()) {
            Text(
              task.shortDescription,
              style = MaterialTheme.typography.bodyLarge,
              color = onContainer.copy(alpha = SECONDARY_TEXT_ALPHA),
              modifier = Modifier.clearAndSetSemantics {},
            )
          }
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ModelCountPill(modelCount, onContainer)
          if (task.newFeature) NewBadge()
          if (task.experimental) ExperimentalIcon(onContainer)
        }
      }
    }
  }
}

@Composable
private fun TaskTile(
  task: Task,
  onClick: () -> Unit,
  sharedBadge: Boolean,
  modifier: Modifier = Modifier,
) {
  val modelCount = rememberModelCount(task)
  val onContainer = getTaskOnBgColor(task)
  val cardDescription = stringResource(R.string.cd_task_card, task.label, modelCount)
  ExpressiveCard(
    onClick = onClick,
    modifier = modifier.semantics { contentDescription = cardDescription },
    containerColor = getTaskBgColor(task),
    shape = MaterialTheme.shapes.extraLargeIncreased,
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        TaskIcon(
          task = task,
          modifier = Modifier.sharedBadge(task.id, enabled = sharedBadge),
          width = TILE_BADGE_SIZE,
        )
        if (task.newFeature) NewBadge()
        if (task.experimental) ExperimentalIcon(onContainer)
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          task.label,
          style = MaterialTheme.typography.titleLargeEmphasized,
          color = onContainer,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        ModelCountLabel(modelCount, onContainer.copy(alpha = SECONDARY_TEXT_ALPHA))
      }
    }
  }
}

@Composable
private fun ExperimentalIcon(tint: Color) {
  Icon(
    painter = painterResource(R.drawable.ic_experiment),
    contentDescription = "Experimental",
    modifier = Modifier.size(20.dp),
    tint = tint,
  )
}

@Composable
private fun NewBadge() {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.customColors.newFeatureContainerColor,
  ) {
    Text(
      "New",
      color = MaterialTheme.customColors.newFeatureTextColor,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
    )
  }
}

@Composable
private fun ModelCountPill(count: Int, contentColor: Color) {
  Surface(shape = CircleShape, color = contentColor.copy(alpha = PILL_ALPHA)) {
    Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
      ModelCountLabel(count, contentColor)
    }
  }
}

@Composable
private fun ModelCountLabel(count: Int, color: Color) {
  val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
  AnimatedContent(
    targetState = count,
    transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
    label = "model count",
  ) { value ->
    Text(
      if (value == 1) "1 Model" else "$value Models",
      style = MaterialTheme.typography.labelLargeEmphasized,
      color = color,
      modifier = Modifier.clearAndSetSemantics {},
    )
  }
}
