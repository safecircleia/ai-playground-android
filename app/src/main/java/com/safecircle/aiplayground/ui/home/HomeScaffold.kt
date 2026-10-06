package com.safecircle.aiplayground.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.data.CategoryInfo
import com.safecircle.aiplayground.data.Task
import kotlinx.coroutines.launch

private val MAX_CONTENT_WIDTH = 840.dp

@Composable
fun HomeScaffold(
  sortedCategories: List<CategoryInfo>,
  tasksByCategory: Map<String, List<Task>>,
  enableAnimation: Boolean,
  sharedBadges: Boolean,
  onTaskClick: (Task) -> Unit,
  onMenuClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  val pagerState = rememberPagerState(pageCount = { sortedCategories.size })
  val scope = rememberCoroutineScope()

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    topBar = { HomeTopBar(scrollBehavior = scrollBehavior, onMenuClick = onMenuClick) },
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(modifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxWidth()) {
        if (sortedCategories.size > 1) {
          CategoryChips(
            categories = sortedCategories,
            selectedIndex = pagerState.currentPage,
            onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
          )
        }
        TaskList(
          pagerState = pagerState,
          sortedCategories = sortedCategories,
          tasksByCategory = tasksByCategory,
          enableAnimation = enableAnimation,
          sharedBadges = sharedBadges,
          onTaskClick = onTaskClick,
        )
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
      }
    }
  }
}
