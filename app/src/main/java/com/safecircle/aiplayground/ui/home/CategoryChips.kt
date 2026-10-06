package com.safecircle.aiplayground.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.data.CategoryInfo

/** Category selector shown above the task list when more than one category exists. */
@Composable
fun CategoryChips(
  categories: List<CategoryInfo>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val listState = rememberLazyListState()
  LaunchedEffect(selectedIndex) { listState.animateScrollToItem(selectedIndex) }
  LazyRow(
    state = listState,
    modifier = modifier.fillMaxWidth(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
      FilterChip(
        selected = index == selectedIndex,
        onClick = { onSelect(index) },
        label = { Text(getCategoryLabel(context, category)) },
        shape = CircleShape,
      )
    }
  }
}

internal fun getCategoryLabel(context: Context, category: CategoryInfo): String {
  val stringRes = category.labelStringRes
  val label = category.label
  if (stringRes != null) {
    return context.getString(stringRes)
  } else if (label != null) {
    return label
  }
  return context.getString(R.string.category_unlabeled)
}
