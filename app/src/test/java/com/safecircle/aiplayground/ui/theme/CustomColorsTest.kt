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
      assertEquals(4, colors.taskOnIconColors.size)
      assertEquals(4, colors.taskOnBgColors.size)
      assertEquals(4, colors.taskIconColors.size)
      assertEquals(4, colors.taskBgGradientColors.size)
      colors.taskBgGradientColors.forEach { assertEquals(2, it.size) }
    }
  }

  @Test
  fun paletteFollowsSchemeRoles() {
    val container = Color(0xFF123456)
    val scheme = lightColorScheme(primaryContainer = container, onPrimary = Color.White)
    val colors = CustomColors().withTaskPalette(scheme)
    assertEquals(container, colors.taskBgColors[0])
    assertEquals(scheme.primary, colors.taskIconColors[0])
    // Badge glyphs sit on the accent colour, so they must use its "on" pair for contrast.
    assertEquals(Color.White, colors.taskOnIconColors[0])
    assertEquals(scheme.onError, colors.taskOnIconColors[3])
    // Text on a task's container card must use that container's "on" pair.
    assertEquals(scheme.onPrimaryContainer, colors.taskOnBgColors[0])
    assertEquals(scheme.onErrorContainer, colors.taskOnBgColors[3])
  }
}
