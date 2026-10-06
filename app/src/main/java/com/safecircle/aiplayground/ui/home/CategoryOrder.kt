package com.safecircle.aiplayground.ui.home

import com.safecircle.aiplayground.data.Category
import com.safecircle.aiplayground.data.CategoryInfo

private val PREDEFINED_CATEGORY_ORDER = listOf(Category.LLM.id, Category.EXPERIMENTAL.id)

/** Predefined categories first (in their fixed order), the rest alphabetically by label. */
internal fun sortCategories(
  categories: List<CategoryInfo>,
  labelOf: (CategoryInfo) -> String,
): List<CategoryInfo> =
  categories.sortedWith(
    compareBy<CategoryInfo> {
        PREDEFINED_CATEGORY_ORDER.indexOf(it.id).let { index ->
          if (index == -1) Int.MAX_VALUE else index
        }
      }
      .thenBy { labelOf(it) }
  )
