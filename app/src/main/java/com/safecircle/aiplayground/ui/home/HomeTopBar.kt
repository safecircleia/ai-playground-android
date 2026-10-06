package com.safecircle.aiplayground.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.ui.common.buildTrackableUrlAnnotatedString
import com.safecircle.aiplayground.ui.theme.heroFontFamily

private const val LITERT_URL = "https://huggingface.co/litert-community"

/** Collapsing app bar: big title + intro at rest, shrinks to a normal bar on scroll. */
@Composable
fun HomeTopBar(scrollBehavior: TopAppBarScrollBehavior, onMenuClick: (() -> Unit)?) {
  val intro = buildAnnotatedString {
    append("${stringResource(R.string.app_intro)} ")
    append(
      buildTrackableUrlAnnotatedString(
        url = LITERT_URL,
        linkText = stringResource(R.string.litert_community_label),
      )
    )
  }
  LargeFlexibleTopAppBar(
    // Wide + heavy Roboto Flex for the brand moment; size comes from the bar's own title style.
    title = {
      Text(
        stringResource(R.string.app_name),
        fontFamily = heroFontFamily,
        fontWeight = FontWeight.ExtraBold,
      )
    },
    subtitle = { Text(intro) },
    navigationIcon = {
      if (onMenuClick != null) {
        IconButton(onClick = onMenuClick) {
          Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.cd_menu))
        }
      }
    },
    scrollBehavior = scrollBehavior,
  )
}
