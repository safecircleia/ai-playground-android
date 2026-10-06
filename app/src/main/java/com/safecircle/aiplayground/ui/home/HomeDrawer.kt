package com.safecircle.aiplayground.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.R

@Composable
fun HomeDrawerContent(onModelsClick: () -> Unit, onSettingsClick: () -> Unit) {
  ModalDrawerSheet {
    Text(
      stringResource(R.string.app_name),
      style = MaterialTheme.typography.titleLargeEmphasized,
      modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
    )
    NavigationDrawerItem(
      label = { Text(stringResource(R.string.drawer_models_label)) },
      icon = { Icon(Icons.AutoMirrored.Rounded.ListAlt, contentDescription = null) },
      selected = false,
      onClick = onModelsClick,
      modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
    NavigationDrawerItem(
      label = { Text(stringResource(R.string.drawer_settings_label)) },
      icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
      selected = false,
      onClick = onSettingsClick,
      modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
  }
}
