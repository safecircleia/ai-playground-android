package com.safecircle.aiplayground.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutTest {
  @Test
  fun boundariesFollowMaterialWindowSizeClasses() {
    assertEquals(HomeLayout.COMPACT, HomeLayout.forWidthDp(0))
    assertEquals(HomeLayout.COMPACT, HomeLayout.forWidthDp(599))
    assertEquals(HomeLayout.MEDIUM, HomeLayout.forWidthDp(600))
    assertEquals(HomeLayout.MEDIUM, HomeLayout.forWidthDp(839))
    assertEquals(HomeLayout.EXPANDED, HomeLayout.forWidthDp(840))
    assertEquals(HomeLayout.EXPANDED, HomeLayout.forWidthDp(1600))
  }

  @Test
  fun negativeWidthIsCompact() {
    assertEquals(HomeLayout.COMPACT, HomeLayout.forWidthDp(-1))
  }
}
