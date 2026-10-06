package com.safecircle.aiplayground.ui.home

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable

private const val MEDIUM_MIN_WIDTH_DP = 600
private const val EXPANDED_MIN_WIDTH_DP = 840

/** Home layout for the current window width (Material window size classes). */
enum class HomeLayout {
  /** Phones: drawer + single pane. */
  COMPACT,
  /** Large phones/small tablets/foldables: navigation rail + single pane. */
  MEDIUM,
  /** Tablets/unfolded: navigation rail + list-detail panes. */
  EXPANDED;

  companion object {
    fun forWidthDp(widthDp: Int): HomeLayout =
      when {
        widthDp >= EXPANDED_MIN_WIDTH_DP -> EXPANDED
        widthDp >= MEDIUM_MIN_WIDTH_DP -> MEDIUM
        else -> COMPACT
      }
  }
}

@Composable
fun rememberHomeLayout(): HomeLayout =
  HomeLayout.forWidthDp(currentWindowAdaptiveInfo().windowSizeClass.minWidthDp)
