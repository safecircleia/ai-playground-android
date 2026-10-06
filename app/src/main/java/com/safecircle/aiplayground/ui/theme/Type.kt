/*
 * Copyright 2026 SafeCircle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.safecircle.aiplayground.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safecircle.aiplayground.R

val appFontFamily =
  FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_extralight, FontWeight.ExtraLight),
    Font(R.font.nunito_light, FontWeight.Light),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
  )

val baseline = Typography()

val AppTypography =
  Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = appFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = appFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = appFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = appFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = appFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = appFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = appFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = appFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = appFontFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = appFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = appFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = appFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = appFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = appFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = appFontFamily),
    displayLargeEmphasized = baseline.displayLargeEmphasized.copy(fontFamily = appFontFamily),
    displayMediumEmphasized = baseline.displayMediumEmphasized.copy(fontFamily = appFontFamily),
    displaySmallEmphasized = baseline.displaySmallEmphasized.copy(fontFamily = appFontFamily),
    headlineLargeEmphasized = baseline.headlineLargeEmphasized.copy(fontFamily = appFontFamily),
    headlineMediumEmphasized = baseline.headlineMediumEmphasized.copy(fontFamily = appFontFamily),
    headlineSmallEmphasized = baseline.headlineSmallEmphasized.copy(fontFamily = appFontFamily),
    titleLargeEmphasized = baseline.titleLargeEmphasized.copy(fontFamily = appFontFamily),
    titleMediumEmphasized = baseline.titleMediumEmphasized.copy(fontFamily = appFontFamily),
    titleSmallEmphasized = baseline.titleSmallEmphasized.copy(fontFamily = appFontFamily),
    bodyLargeEmphasized = baseline.bodyLargeEmphasized.copy(fontFamily = appFontFamily),
    bodyMediumEmphasized = baseline.bodyMediumEmphasized.copy(fontFamily = appFontFamily),
    bodySmallEmphasized = baseline.bodySmallEmphasized.copy(fontFamily = appFontFamily),
    labelLargeEmphasized = baseline.labelLargeEmphasized.copy(fontFamily = appFontFamily),
    labelMediumEmphasized = baseline.labelMediumEmphasized.copy(fontFamily = appFontFamily),
    labelSmallEmphasized = baseline.labelSmallEmphasized.copy(fontFamily = appFontFamily),
  )

val titleMediumNarrow =
  baseline.titleMedium.copy(fontFamily = appFontFamily, letterSpacing = 0.0.sp)

val titleSmaller =
  baseline.titleSmall.copy(
    fontFamily = appFontFamily,
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
  )

val labelSmallNarrow = baseline.labelSmall.copy(fontFamily = appFontFamily, letterSpacing = 0.0.sp)

val labelSmallNarrowMedium =
  baseline.labelSmall.copy(
    fontFamily = appFontFamily,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.0.sp,
  )

val bodySmallNarrow = baseline.bodySmall.copy(fontFamily = appFontFamily, letterSpacing = 0.0.sp)

val bodySmallMediumNarrow =
  baseline.bodySmall.copy(fontFamily = appFontFamily, letterSpacing = 0.0.sp, fontSize = 14.sp)

val bodySmallMediumNarrowBold =
  baseline.bodySmall.copy(
    fontFamily = appFontFamily,
    letterSpacing = 0.0.sp,
    fontSize = 14.sp,
    fontWeight = FontWeight.Bold,
  )

val homePageTitleStyle =
  baseline.displayMedium.copy(
    fontFamily = appFontFamily,
    fontSize = 48.sp,
    lineHeight = 48.sp,
    letterSpacing = -1.sp,
    fontWeight = FontWeight.Medium,
  )

val bodyLargeNarrow = baseline.bodyLarge.copy(letterSpacing = 0.2.sp)
val bodyMediumMedium = baseline.bodyMedium.copy(fontWeight = FontWeight.Medium)

val headlineLargeMedium = baseline.headlineLarge.copy(fontWeight = FontWeight.Medium)

val emptyStateTitle = baseline.headlineSmall.copy(fontSize = 37.sp, lineHeight = 50.sp)
val emptyStateContent = baseline.headlineSmall.copy(fontSize = 16.sp, lineHeight = 22.sp)

// Material 3 shape scale (dp corners, incl. the expressive "increased" steps).
val AppShapes =
  Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    largeIncreased = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
  )
