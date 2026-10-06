package com.safecircle.aiplayground.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.BuildConfig
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.ui.theme.heroFontFamily

private val LOGO_BADGE_SIZE = 56.dp
private const val VERSION_PILL_ALPHA = 0.14f

@Composable
fun HomeDrawerContent(
  onHomeClick: () -> Unit,
  onModelsClick: () -> Unit,
  onSettingsClick: () -> Unit,
) {
  ModalDrawerSheet {
    DrawerHeader()
    Spacer(Modifier.height(8.dp))
    DrawerItem(
      label = stringResource(R.string.home_label),
      icon = Icons.Rounded.Home,
      selected = true,
      onClick = onHomeClick,
    )
    DrawerItem(
      label = stringResource(R.string.drawer_models_label),
      icon = Icons.AutoMirrored.Rounded.ListAlt,
      selected = false,
      onClick = onModelsClick,
    )
    DrawerItem(
      label = stringResource(R.string.drawer_settings_label),
      icon = Icons.Rounded.Settings,
      selected = false,
      onClick = onSettingsClick,
    )
  }
}

/** Brand block: logo on an expressive shape, wide heavy title, and the app version. */
@Composable
private fun DrawerHeader() {
  val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
  Surface(
    modifier = Modifier.fillMaxWidth().padding(12.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    shape = MaterialTheme.shapes.extraLargeIncreased,
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier =
          Modifier.size(LOGO_BADGE_SIZE)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          painterResource(R.drawable.logo),
          contentDescription = null,
          modifier = Modifier.size(28.dp),
          tint = Color.Unspecified,
        )
      }
      Text(
        stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        fontFamily = heroFontFamily,
        fontWeight = FontWeight.ExtraBold,
        color = onContainer,
      )
      Surface(shape = CircleShape, color = onContainer.copy(alpha = VERSION_PILL_ALPHA)) {
        Text(
          "v${BuildConfig.VERSION_NAME}",
          style = MaterialTheme.typography.labelLargeEmphasized,
          color = onContainer,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
      }
    }
  }
}

@Composable
private fun DrawerItem(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
) {
  NavigationDrawerItem(
    label = { Text(label, style = MaterialTheme.typography.titleMedium) },
    icon = { Icon(icon, contentDescription = null) },
    selected = selected,
    onClick = onClick,
    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
  )
}
