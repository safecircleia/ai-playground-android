package com.safecircle.aiplayground.ui.home

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.R

/** Navigation rail used instead of the drawer on medium and expanded windows. */
@Composable
fun HomeRail(
  onModelsClick: () -> Unit,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationRail(
    modifier = modifier,
    header = {
      Icon(
        painterResource(R.drawable.logo),
        contentDescription = null,
        modifier = Modifier.size(32.dp),
        tint = Color.Unspecified,
      )
    },
  ) {
    NavigationRailItem(
      selected = true,
      onClick = {},
      icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
      label = { Text(stringResource(R.string.home_label)) },
    )
    NavigationRailItem(
      selected = false,
      onClick = onModelsClick,
      icon = { Icon(Icons.AutoMirrored.Rounded.ListAlt, contentDescription = null) },
      label = { Text(stringResource(R.string.drawer_models_label)) },
    )
    NavigationRailItem(
      selected = false,
      onClick = onSettingsClick,
      icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
      label = { Text(stringResource(R.string.drawer_settings_label)) },
    )
  }
}
