package com.safecircle.aiplayground.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomColorsTest {
  // Palette lists are indexed with `task.index % size`; every list must have exactly 4 entries.
  @Test
  fun paletteListsHaveFourEntriesForStaticLightAndDarkSchemes() {
    for (scheme in listOf(lightColorScheme(), darkColorScheme())) {
      val colors = CustomColors().withTaskPalette(scheme)
      assertEquals(4, colors.taskBgColors.size)
      assertEquals(4, colors.taskOnBgColors.size)
      assertEquals(4, colors.taskIconColors.size)
      assertEquals(4, colors.taskBgGradientColors.size)
      colors.taskBgGradientColors.forEach { assertEquals(2, it.size) }
    }
  }

  @Test
  fun paletteFollowsSchemeRoles() {
    val container = Color(0xFF123456)
    val scheme = lightColorScheme(primaryContainer = container, onPrimaryContainer = Color.White)
    val colors = CustomColors().withTaskPalette(scheme)
    assertEquals(container, colors.taskBgColors[0])
    assertEquals(Color.White, colors.taskOnBgColors[0])
    assertEquals(scheme.primary, colors.taskIconColors[0])
  }
}
