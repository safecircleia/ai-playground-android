package com.safecircle.aiplayground.ui.home

import com.safecircle.aiplayground.data.Category
import com.safecircle.aiplayground.data.CategoryInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryOrderTest {
  private val labels =
    mapOf("llm" to "LLM", "experimental" to "Experimental", "b" to "Bravo", "a" to "Alpha")

  private fun sort(vararg categories: CategoryInfo) =
    sortCategories(categories.toList()) { labels.getValue(it.id) }.map { it.id }

  @Test
  fun predefinedCategoriesComeFirstInPredefinedOrder() {
    val other = CategoryInfo(id = "a")
    assertEquals(listOf("llm", "experimental", "a"), sort(other, Category.EXPERIMENTAL, Category.LLM))
  }

  // Regression for fd0d3c2: two categories outside the predefined order must not crash.
  @Test
  fun twoUnknownCategoriesSortByLabel() {
    assertEquals(listOf("a", "b"), sort(CategoryInfo(id = "b"), CategoryInfo(id = "a")))
  }

  @Test
  fun unknownCategoriesGoAfterPredefinedOnes() {
    assertEquals(
      listOf("experimental", "a", "b"),
      sort(CategoryInfo(id = "b"), CategoryInfo(id = "a"), Category.EXPERIMENTAL),
    )
  }
}
