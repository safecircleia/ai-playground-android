package com.safecircle.aiplayground.ui.common.expressive

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.safecircle.aiplayground.ui.theme.GalleryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExpressiveCardTest {
  @get:Rule val rule = createComposeRule()

  @Test
  fun clickInvokesCallbackOnce() {
    var clicks = 0
    rule.setContent {
      GalleryTheme { ExpressiveCard(onClick = { clicks++ }) { Text("Hello card") } }
    }
    rule.onNodeWithText("Hello card").performClick()
    assertEquals(1, clicks)
  }
}
