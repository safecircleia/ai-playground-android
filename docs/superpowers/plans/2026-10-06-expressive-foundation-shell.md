# Material 3 Expressive: Foundation + Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the app a Material 3 Expressive look and motion (theme, shapes, springs, shared-element and predictive-back transitions) and an adaptive home (rail at >=600dp, list-detail at >=840dp), without changing app behavior.

**Architecture:** Switch `GalleryTheme` to `MaterialExpressiveTheme` on Material3 `1.5.0-alpha27`, derive the task colour palette from the active `ColorScheme` (so it follows dynamic colour), add three small shared components (`ExpressiveCard`, `ShapeBadge`, shared-element helpers), split the 1171-line `HomeScreen.kt` into focused files while redesigning it, replace the hand-rolled nav transitions with `MotionScheme` springs, then add an adaptive layer around Home.

**Tech Stack:** Kotlin 2.4.20, Compose BOM 2026.09.00, `androidx.compose.material3` 1.5.0-alpha27, `androidx.compose.material3.adaptive` 1.3.0, navigation-compose 2.10.2, Compose shared transitions (animation 1.12.1).

**Spec:** `docs/superpowers/specs/2026-10-06-expressive-foundation-shell-design.md` (read its "Plan-time adjustments" section first; it overrides the original text where they differ).

## Global Constraints

- Compose-only; the only new dependencies are AndroidX (`material3` bumped to `1.5.0-alpha27`, plus `adaptive`, `adaptive-layout`, `adaptive-navigation` at `1.3.0`). No third-party UI libraries.
- Dynamic colour on API 31+, static scheme below it (`minSdk = 30`). The AMOLED override stays.
- App font stays Nunito (`appFontFamily`).
- Existing behaviour is unchanged: model download, chat, classification, benchmark, TOS dialog, settings dialog, notification permission request, deep links.
- Chat, Prompt Lab, Safety Detection, Benchmark, model manager screens and dialogs are not redesigned here; they only inherit theme tokens.
- No version bump and no release. Work happens on branch `feat/m3-expressive-foundation-shell` in small commits.
- Commit messages end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Build/test commands are noisy; pipe through `tail -40` (or run them via the sandboxed shell tool) and read the real result. Never claim a step passed without its output.

## Review Focus

Failure modes the spec implies but a happy-path build would not catch, most likely first:

1. **Palette index maths.** `Task.index` defaults to `-1`; palette lists are indexed with `index % size`. Every palette list must have exactly 4 entries (gradient entries exactly 2) in both light and dark static schemes. Pinned by `CustomColorsTest` (Task 2).
2. **Window-size boundaries.** 599dp vs 600dp vs 839dp vs 840dp must pick COMPACT/MEDIUM/EXPANDED exactly. Pinned by `HomeLayoutTest` (Task 6).
3. **Category ordering regression.** Commit `fd0d3c2` fixed a crash when two categories are outside the predefined order; the logic is being moved. Pinned by `CategoryOrderTest` (Task 4).
4. **Reduced motion.** With `animator_duration_scale = 0`, cards must still appear and navigation must still complete. Manual check in Task 7.
5. **Resize with a selection.** Shrinking EXPANDED -> COMPACT (fold/rotate) with a task selected must not crash or lose the selected task. Manual check in Task 7, plus `configChanges` so the activity is not recreated (Task 6).

Also checked manually in Task 7: font scale 2.0 in the large app bar; AMOLED theme.

## File Structure

| File | Responsibility | Task |
|---|---|---|
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | Material3 alpha + adaptive deps, global opt-ins | 1 |
| `ui/theme/Theme.kt` | `MaterialExpressiveTheme`, palette derivation | 1, 2 |
| `ui/theme/Type.kt` | Dp-based expressive shape scale, emphasized type | 2 |
| `ui/common/ColorUtils.kt` | add `getTaskOnBgColor` | 3 |
| `ui/common/expressive/ExpressiveCard.kt` | Card with spring press scale | 3 |
| `ui/common/expressive/ShapeBadge.kt` | MaterialShapes badge | 3 |
| `ui/common/expressive/Entrance.kt` | `staggeredEntrance` modifier | 3 |
| `ui/common/expressive/SharedTransition.kt` | shared-element locals + `sharedBadge` modifier | 3 |
| `ui/common/TaskIcon.kt` | re-implemented on `ShapeBadge` (same signature) | 3 |
| `ui/home/HomeScreen.kt` | state, loading, dialogs, drawer host (shrinks 1171 -> ~250 lines) | 4, 6 |
| `ui/home/HomeScaffold.kt` | Scaffold + collapsing bar + scrolling content | 4 |
| `ui/home/HomeTopBar.kt` | `LargeFlexibleTopAppBar` | 4 |
| `ui/home/TaskCards.kt` | `TaskList`, `TaskCard` | 4 |
| `ui/home/CategoryChips.kt` | filter chips, `getCategoryLabel` | 4 |
| `ui/home/CategoryOrder.kt` | pure `sortCategories` | 4 |
| `ui/home/HomeDrawer.kt` | modal drawer content | 4 |
| `ui/home/SquareDrawerItem.kt` | deleted | 4 |
| `ui/navigation/NavTransitions.kt` | motion-scheme transitions | 5 |
| `ui/navigation/GalleryNavGraph.kt` | use `NavTransitions`, shared transition layout, adaptive home route | 4, 5, 6 |
| `GalleryAppTopBar.kt`, `ui/modelmanager/ModelManager.kt` | `titleIcon` slot; `embedded` mode | 5, 6 |
| `ui/home/HomeLayout.kt` | window width -> layout enum | 6 |
| `ui/home/HomeRail.kt`, `ui/home/HomeListDetail.kt` | rail, list-detail scaffold | 6 |
| `AndroidManifest.xml`, `res/values/strings.xml` | `configChanges`, two strings | 6 |

---

### Task 1: Material3 1.5.0-alpha27 + `MaterialExpressiveTheme`

This is the compile spike. If this task cannot build, stop and report to the user; do not continue.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/safecircle/aiplayground/ui/theme/Theme.kt`

**Interfaces:**
- Produces: project compiles against `androidx.compose.material3:material3:1.5.0-alpha27` with expressive APIs (`MaterialExpressiveTheme`, `MotionScheme.expressive()`, `MaterialShapes`, `LoadingIndicator`, `LargeFlexibleTopAppBar`, `MaterialTheme.motionScheme`) and the adaptive libs; expressive opt-ins are global so later tasks need no `@OptIn`.

- [ ] **Step 1: Bump versions in `gradle/libs.versions.toml`**

In `[versions]`, after `navigation = "2.10.2"` add:

```toml
material3 = "1.5.0-alpha27"
adaptive = "1.3.0"
```

Replace the line `androidx-material3 = { group = "androidx.compose.material3", name = "material3" }` with:

```toml
androidx-material3 = { group = "androidx.compose.material3", name = "material3", version.ref = "material3" }
androidx-adaptive = { group = "androidx.compose.material3.adaptive", name = "adaptive", version.ref = "adaptive" }
androidx-adaptive-layout = { group = "androidx.compose.material3.adaptive", name = "adaptive-layout", version.ref = "adaptive" }
androidx-adaptive-navigation = { group = "androidx.compose.material3.adaptive", name = "adaptive-navigation", version.ref = "adaptive" }
```

- [ ] **Step 2: Add dependencies and global opt-ins in `app/build.gradle.kts`**

After `implementation(libs.androidx.material3)` add:

```kotlin
  implementation(libs.androidx.adaptive)
  implementation(libs.androidx.adaptive.layout)
  implementation(libs.androidx.adaptive.navigation)
```

Replace the `kotlin { compilerOptions { ... } }` block with:

```kotlin
kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    optIn.addAll(
      "androidx.compose.material3.ExperimentalMaterial3Api",
      "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
      "androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
      "androidx.compose.animation.ExperimentalSharedTransitionApi",
    )
  }
}
```

- [ ] **Step 3: Switch the theme in `Theme.kt`**

Add imports `androidx.compose.material3.MaterialExpressiveTheme` and `androidx.compose.material3.MotionScheme`. Replace the line

```kotlin
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, shapes = AppShapes, content = content)
```

with

```kotlin
    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      motionScheme = MotionScheme.expressive(),
      shapes = AppShapes,
      typography = AppTypography,
      content = content,
    )
```

(`MaterialTheme` stays imported; `MaterialTheme.customColors` still uses it.)

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug 2>&1 | tail -40`
Expected: `BUILD SUCCESSFUL`.
If `RoundedPolygon`/`androidx.graphics.shapes` types are unresolved later (Task 3), add `androidx.graphics:graphics-shapes:1.1.0` as an `implementation` dependency (the 1.1.0 artifact is already in the Gradle cache).
If the build fails on alpha incompatibility with Kotlin 2.4.20 / AGP 9.4.1 that cannot be fixed by a one-line change: **stop, `git checkout -- .`, and report** (fallback is staying on 1.4.0, which lacks `MaterialShapes`, `LoadingIndicator` and public `MotionScheme.expressive()`; that needs a re-plan).

- [ ] **Step 5: Smoke-run on the emulator**

```bash
./gradlew :app:installDebug 2>&1 | tail -5
adb shell monkey -p com.safecircle.aiplayground -c android.intent.category.LAUNCHER 1
sleep 4 && adb exec-out screencap -p > /tmp/task1-home.png
```

Open `/tmp/task1-home.png`. Expected: the existing home screen renders (TOS dialog first on a fresh install; accept it). No crash in `adb logcat -d | grep -i "FATAL"`.

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/safecircle/aiplayground/ui/theme/Theme.kt
git commit -m "feat: move to Material3 1.5.0-alpha27 and MaterialExpressiveTheme

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Theme tokens (palette from scheme, dp shapes, emphasized type)

**Files:**
- Modify: `app/src/main/java/com/safecircle/aiplayground/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/safecircle/aiplayground/ui/theme/Type.kt`
- Test: `app/src/test/java/com/safecircle/aiplayground/ui/theme/CustomColorsTest.kt`

**Interfaces:**
- Produces: `fun CustomColors.withTaskPalette(scheme: ColorScheme): CustomColors` (fills `taskCardBgColor`, `taskBgColors`, `taskOnBgColors`, `taskBgGradientColors`, `taskIconColors` from theme roles; all lists have 4 entries, gradient entries have 2); new field `CustomColors.taskOnBgColors: List<Color>`; `AppShapes` with dp corners incl. `largeIncreased`, `extraLargeIncreased`, `extraExtraLarge`; `AppTypography` with all 15 `*Emphasized` styles in Nunito.
- Note: current `RoundedCornerShape(4)` etc. are **percent** corners (a latent bug). Dp corners change the look of out-of-scope screens slightly; that is intended.

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*CustomColorsTest' 2>&1 | tail -20`
Expected: FAIL to compile (`withTaskPalette` / `taskOnBgColors` unresolved).

- [ ] **Step 3: Implement the palette in `Theme.kt`**

Add `val taskOnBgColors: List<Color> = listOf(),` to `CustomColors` (next to `taskBgColors`).

Delete these four properties from **both** `lightCustomColors` and `darkCustomColors`: `taskCardBgColor`, `taskBgColors`, `taskBgGradientColors`, `taskIconColors` (light: just under `appTitleGradientColors/tabHeaderBgColor`, dark: same position). They are always supplied by `withTaskPalette` now.

Add (below `val MaterialTheme.customColors ...`):

```kotlin
private const val GRADIENT_END_BLEND = 0.3f

/**
 * Fills the task palette from the active [ColorScheme] so it follows dynamic colour, dark mode
 * and contrast settings. Index order matches the old palette: tasks cycle through 4 accents.
 */
fun CustomColors.withTaskPalette(scheme: ColorScheme): CustomColors {
  val accents = listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.error)
  return copy(
    taskCardBgColor = scheme.surfaceContainerLow,
    taskBgColors =
      listOf(
        scheme.primaryContainer,
        scheme.tertiaryContainer,
        scheme.secondaryContainer,
        scheme.errorContainer,
      ),
    taskOnBgColors =
      listOf(
        scheme.onPrimaryContainer,
        scheme.onTertiaryContainer,
        scheme.onSecondaryContainer,
        scheme.onErrorContainer,
      ),
    taskBgGradientColors = accents.map { listOf(it, lerp(it, scheme.surface, GRADIENT_END_BLEND)) },
    taskIconColors = accents,
  )
}
```

Add imports `androidx.compose.material3.ColorScheme` and `androidx.compose.ui.graphics.lerp`.

In `GalleryTheme`, replace

```kotlin
  val customColorsPalette = if (darkTheme) darkCustomColors else lightCustomColors
```

with

```kotlin
  val customColorsPalette =
    (if (darkTheme) darkCustomColors else lightCustomColors).withTaskPalette(colorScheme)
```

- [ ] **Step 4: Shapes and typography in `Type.kt`**

Add `import androidx.compose.ui.unit.dp`. Replace `AppShapes` with:

```kotlin
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
```

In `AppTypography`, after `labelSmall = ...,` add:

```kotlin
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
```

- [ ] **Step 5: Run the test and confirm it passes, then build**

Run: `./gradlew :app:testDebugUnitTest --tests '*CustomColorsTest' 2>&1 | tail -20` -> Expected PASS (2 tests).
Run: `./gradlew :app:assembleDebug 2>&1 | tail -20` -> Expected BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/safecircle/aiplayground/ui/theme app/src/test/java/com/safecircle/aiplayground/ui/theme
git commit -m "feat: derive task palette from the colour scheme, dp shape scale, emphasized type

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Shared expressive components

**Files:**
- Create: `app/src/main/java/com/safecircle/aiplayground/ui/common/expressive/ExpressiveCard.kt`
- Create: `app/src/main/java/com/safecircle/aiplayground/ui/common/expressive/ShapeBadge.kt`
- Create: `app/src/main/java/com/safecircle/aiplayground/ui/common/expressive/Entrance.kt`
- Create: `app/src/main/java/com/safecircle/aiplayground/ui/common/expressive/SharedTransition.kt`
- Modify: `app/src/main/java/com/safecircle/aiplayground/ui/common/TaskIcon.kt`
- Modify: `app/src/main/java/com/safecircle/aiplayground/ui/common/ColorUtils.kt`
- Test: `app/src/androidTest/java/com/safecircle/aiplayground/ui/common/expressive/ExpressiveCardTest.kt`

**Interfaces:**
- Consumes: `CustomColors.taskBgColors/taskOnBgColors` (Task 2).
- Produces (all in package `com.safecircle.aiplayground.ui.common.expressive`):
  - `fun ExpressiveCard(onClick: () -> Unit, modifier: Modifier = Modifier, containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow, shape: Shape = MaterialTheme.shapes.extraLargeIncreased, content: @Composable ColumnScope.() -> Unit)`
  - `fun ShapeBadge(index: Int, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier, size: Dp = 56.dp, content: @Composable () -> Unit)`
  - `fun Modifier.staggeredEntrance(index: Int, enabled: Boolean): Modifier` (composable)
  - `val LocalSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope?>`, `val LocalAnimatedVisibilityScope: ProvidableCompositionLocal<AnimatedVisibilityScope?>`, `fun Modifier.sharedBadge(taskId: String, enabled: Boolean = true): Modifier` (composable)
  - `ui.common.getTaskOnBgColor(task: Task): Color`
  - `TaskIcon(task, modifier, width, animationProgress)` keeps its existing signature.

- [ ] **Step 1: Write the failing instrumented test**

```kotlin
package com.safecircle.aiplayground.ui.common.expressive

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
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
```

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.safecircle.aiplayground.ui.common.expressive.ExpressiveCardTest 2>&1 | tail -25`
Expected: FAIL to compile (`ExpressiveCard` unresolved). Emulator `emulator-5554` must be attached (`adb devices`).

- [ ] **Step 3: `ExpressiveCard.kt`**

```kotlin
package com.safecircle.aiplayground.ui.common.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

private const val PRESSED_SCALE = 0.97f

/** A clickable card that springs down slightly while pressed (M3 Expressive spatial motion). */
@Composable
fun ExpressiveCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
  shape: Shape = MaterialTheme.shapes.extraLargeIncreased,
  content: @Composable ColumnScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (pressed) PRESSED_SCALE else 1f,
      animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
      label = "card press scale",
    )
  Card(
    onClick = onClick,
    modifier = modifier.graphicsLayer {
      scaleX = scale
      scaleY = scale
    },
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    interactionSource = interactionSource,
    content = content,
  )
}
```

- [ ] **Step 4: `ShapeBadge.kt`**

```kotlin
package com.safecircle.aiplayground.ui.common.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Square-ish expressive shapes; tasks cycle through them by index.
private val BadgePolygons =
  listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.SoftBurst,
    MaterialShapes.Flower,
  )

/** An icon container clipped to one of the Material expressive shapes. */
@Composable
fun ShapeBadge(
  index: Int,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
  content: @Composable () -> Unit,
) {
  val shape = BadgePolygons[index.coerceAtLeast(0) % BadgePolygons.size].toShape()
  Box(
    modifier = modifier.size(size).clip(shape).background(containerColor),
    contentAlignment = Alignment.Center,
  ) {
    CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
  }
}
```

- [ ] **Step 5: `Entrance.kt`**

```kotlin
package com.safecircle.aiplayground.ui.common.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val STAGGER_DELAY_MS = 60L
private const val ENTRANCE_OFFSET_DP = 24

/**
 * Fades and slides an item up on first composition, delayed by [index] so a list cascades in.
 * When [enabled] is false the item is shown immediately.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int, enabled: Boolean): Modifier {
  var shown by remember { mutableStateOf(!enabled) }
  LaunchedEffect(enabled) {
    if (enabled) {
      delay(index * STAGGER_DELAY_MS)
      shown = true
    }
  }
  val progress by
    animateFloatAsState(
      targetValue = if (shown) 1f else 0f,
      animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
      label = "staggered entrance",
    )
  return graphicsLayer {
    alpha = progress.coerceIn(0f, 1f)
    translationY = ENTRANCE_OFFSET_DP.dp.toPx() * (1f - progress)
  }
}
```

- [ ] **Step 6: `SharedTransition.kt`**

```kotlin
package com.safecircle.aiplayground.ui.common.expressive

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Marks a task icon as a shared element between the home card and the model list header. A no-op
 * outside a shared-transition layout, or when [enabled] is false (e.g. two panes on screen at once,
 * where the same key would appear twice).
 */
@Composable
fun Modifier.sharedBadge(taskId: String, enabled: Boolean = true): Modifier {
  val sharedScope = LocalSharedTransitionScope.current
  val visibilityScope = LocalAnimatedVisibilityScope.current
  if (!enabled || sharedScope == null || visibilityScope == null) return this
  return with(sharedScope) {
    this@sharedBadge.sharedElement(
      sharedContentState = rememberSharedContentState(key = "task-badge-$taskId"),
      animatedVisibilityScope = visibilityScope,
    )
  }
}
```

- [ ] **Step 7: `ColorUtils.kt` and `TaskIcon.kt`**

Append to `ColorUtils.kt`:

```kotlin
@Composable
fun getTaskOnBgColor(task: Task): Color {
  val colors = MaterialTheme.customColors.taskOnBgColors
  return colors[task.index.coerceAtLeast(0) % colors.size]
}
```

Replace the whole body of `TaskIcon.kt` after the license/package line with:

```kotlin
package com.safecircle.aiplayground.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.ui.common.expressive.ShapeBadge

/**
 * Icon representing a task: a glyph on an expressive-shape badge coloured from the theme.
 * [animationProgress] (0..1) scales/rotates/fades the badge in.
 */
@Composable
fun TaskIcon(
  task: Task,
  modifier: Modifier = Modifier,
  width: Dp = 56.dp,
  animationProgress: Float = 1f,
) {
  ShapeBadge(
    index = task.index,
    containerColor = getTaskBgColor(task),
    contentColor = getTaskOnBgColor(task),
    size = width,
    modifier =
      modifier.graphicsLayer {
        alpha = animationProgress
        scaleX = 0.6f + 0.4f * animationProgress
        scaleY = 0.6f + 0.4f * animationProgress
        rotationZ = -90f * (1f - animationProgress)
      },
  ) {
    Icon(
      task.icon ?: ImageVector.vectorResource(task.iconVectorResourceId!!),
      contentDescription = null,
      modifier = Modifier.size(width * 0.5f),
    )
  }
}
```

(Keep the license header comment at the top of the file. `R.drawable.circle` etc. are still used by the loaders; do not delete drawables.)

- [ ] **Step 8: Build and run the test**

Run: `./gradlew :app:assembleDebug 2>&1 | tail -30` -> BUILD SUCCESSFUL (fix any import errors the compiler reports).
Run the Step 2 command again -> Expected PASS.
Run `adb logcat -d | grep -i FATAL` -> empty.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/safecircle/aiplayground/ui/common app/src/androidTest
git commit -m "feat: add ExpressiveCard, ShapeBadge, entrance and shared-element helpers

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Redesigned, split Home (compact layout)

**Files:**
- Create: `ui/home/CategoryOrder.kt`, `ui/home/CategoryChips.kt`, `ui/home/TaskCards.kt`, `ui/home/HomeTopBar.kt`, `ui/home/HomeScaffold.kt`, `ui/home/HomeDrawer.kt`
- Rewrite: `ui/home/HomeScreen.kt`
- Delete: `ui/home/SquareDrawerItem.kt`
- Modify: `ui/navigation/GalleryNavGraph.kt` (HomeScreen call), `ui/theme/Theme.kt` (remove dead fields), `ui/theme/Type.kt` (remove `homePageTitleStyle` if unused)
- Test: `app/src/test/java/com/safecircle/aiplayground/ui/home/CategoryOrderTest.kt`

(All paths under `app/src/main/java/com/safecircle/aiplayground/` unless they start with `app/`.)

**Interfaces:**
- Consumes: `ExpressiveCard`, `staggeredEntrance`, `sharedBadge`, `TaskIcon` (Task 3).
- Produces:
  - `internal fun sortCategories(categories: List<CategoryInfo>, labelOf: (CategoryInfo) -> String): List<CategoryInfo>`
  - `internal fun getCategoryLabel(context: Context, category: CategoryInfo): String`
  - `@Composable fun CategoryChips(categories: List<CategoryInfo>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun TaskList(pagerState: PagerState, sortedCategories: List<CategoryInfo>, tasksByCategory: Map<String, List<Task>>, enableAnimation: Boolean, sharedBadges: Boolean, onTaskClick: (Task) -> Unit)`
  - `@Composable fun HomeTopBar(scrollBehavior: TopAppBarScrollBehavior, onMenuClick: (() -> Unit)?)`
  - `@Composable fun HomeScaffold(sortedCategories, tasksByCategory, enableAnimation, sharedBadges, onTaskClick, onMenuClick: (() -> Unit)?, modifier: Modifier = Modifier)`
  - `@Composable fun HomeDrawerContent(onModelsClick: () -> Unit, onSettingsClick: () -> Unit)`
  - `HomeScreen(modelManagerViewModel, tosViewModel, navigateToTaskScreen, onModelsClicked, enableAnimation, modifier)` (the unused `onNotificationsClicked` and dead `gm4` params are gone).
- Dead code removed on purpose: `gm4` (the only caller passes `false`), `AppTitle*`, `TryGm4IntroText`, the square/grid card variant, `SquareDrawerItem`.

- [ ] **Step 1: Write the failing test `CategoryOrderTest.kt`**

```kotlin
package com.safecircle.aiplayground.ui.home

import com.safecircle.aiplayground.data.Category
import com.safecircle.aiplayground.data.CategoryInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryOrderTest {
  private val labels = mapOf("llm" to "LLM", "experimental" to "Experimental", "b" to "Bravo", "a" to "Alpha")
  private fun sort(vararg c: CategoryInfo) = sortCategories(c.toList()) { labels.getValue(it.id) }.map { it.id }

  @Test
  fun predefinedCategoriesComeFirstInPredefinedOrder() {
    val other = CategoryInfo(id = "a")
    assertEquals(listOf("llm", "experimental", "a"), sort(other, Category.EXPERIMENTAL, Category.LLM))
  }

  // Regression for fd0d3c2: two categories outside the predefined order must not crash.
  @Test
  fun twoUnknownCategoriesSortByLabel() {
    assertEquals(listOf("a", "b"), sort(CategoryInfo(id = "b"), CategoryInfo(id = "a")))
  }

  @Test
  fun unknownCategoriesGoAfterPredefinedOnes() {
    assertEquals(listOf("experimental", "a", "b"), sort(CategoryInfo(id = "b"), CategoryInfo(id = "a"), Category.EXPERIMENTAL))
  }
}
```

If `CategoryInfo` has other required constructor params, pass them; `id` is the only one without a default as of this writing.

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*CategoryOrderTest' 2>&1 | tail -20` -> FAIL (unresolved `sortCategories`).

- [ ] **Step 3: `CategoryOrder.kt`**

```kotlin
package com.safecircle.aiplayground.ui.home

import com.safecircle.aiplayground.data.Category
import com.safecircle.aiplayground.data.CategoryInfo

private val PREDEFINED_CATEGORY_ORDER = listOf(Category.LLM.id, Category.EXPERIMENTAL.id)

/** Predefined categories first (in their fixed order), the rest alphabetically by label. */
internal fun sortCategories(
  categories: List<CategoryInfo>,
  labelOf: (CategoryInfo) -> String,
): List<CategoryInfo> =
  categories.sortedWith(
    compareBy<CategoryInfo> {
        PREDEFINED_CATEGORY_ORDER.indexOf(it.id).let { index -> if (index == -1) Int.MAX_VALUE else index }
      }
      .thenBy { labelOf(it) }
  )
```

Run the Step 2 command -> PASS (3 tests).

- [ ] **Step 4: `CategoryChips.kt`**

```kotlin
package com.safecircle.aiplayground.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.data.CategoryInfo

/** Category selector shown above the task list when more than one category exists. */
@Composable
fun CategoryChips(
  categories: List<CategoryInfo>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val listState = rememberLazyListState()
  LaunchedEffect(selectedIndex) { listState.animateScrollToItem(selectedIndex) }
  LazyRow(
    state = listState,
    modifier = modifier.fillMaxWidth(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
      FilterChip(
        selected = index == selectedIndex,
        onClick = { onSelect(index) },
        label = { Text(getCategoryLabel(context, category)) },
        shape = CircleShape,
      )
    }
  }
}

internal fun getCategoryLabel(context: Context, category: CategoryInfo): String {
  val stringRes = category.labelStringRes
  val label = category.label
  if (stringRes != null) {
    return context.getString(stringRes)
  } else if (label != null) {
    return label
  }
  return context.getString(R.string.category_unlabeled)
}
```

- [ ] **Step 5: `TaskCards.kt`**

```kotlin
package com.safecircle.aiplayground.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.data.CategoryInfo
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.ui.common.TaskIcon
import com.safecircle.aiplayground.ui.common.expressive.ExpressiveCard
import com.safecircle.aiplayground.ui.common.expressive.sharedBadge
import com.safecircle.aiplayground.ui.common.expressive.staggeredEntrance
import com.safecircle.aiplayground.ui.theme.customColors

/** One page of task cards per category. */
@Composable
fun TaskList(
  pagerState: PagerState,
  sortedCategories: List<CategoryInfo>,
  tasksByCategory: Map<String, List<Task>>,
  enableAnimation: Boolean,
  sharedBadges: Boolean,
  onTaskClick: (Task) -> Unit,
) {
  HorizontalPager(
    state = pagerState,
    verticalAlignment = Alignment.Top,
    contentPadding = PaddingValues(horizontal = 16.dp),
    pageSpacing = 16.dp,
  ) { page ->
    val tasks = tasksByCategory[sortedCategories[page].id].orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      tasks.forEachIndexed { index, task ->
        TaskCard(
          task = task,
          onClick = { onTaskClick(task) },
          sharedBadge = sharedBadges,
          modifier =
            Modifier.fillMaxWidth().staggeredEntrance(index, enabled = enableAnimation && page == 0),
        )
      }
    }
  }
}

@Composable
fun TaskCard(
  task: Task,
  onClick: () -> Unit,
  sharedBadge: Boolean,
  modifier: Modifier = Modifier,
) {
  // Reading updateTrigger makes the count recompose when models are added/removed.
  val modelCount by remember {
    derivedStateOf { if (task.updateTrigger.value >= 0) task.models.size else 0 }
  }
  val cardDescription = stringResource(R.string.cd_task_card, task.label, modelCount)
  ExpressiveCard(
    onClick = onClick,
    modifier = modifier.semantics { contentDescription = cardDescription },
    containerColor = MaterialTheme.customColors.taskCardBgColor,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      TaskIcon(
        task = task,
        modifier = Modifier.sharedBadge(task.id, enabled = sharedBadge),
        width = 56.dp,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            task.label,
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (task.newFeature) NewBadge()
          if (task.experimental) {
            Icon(
              painter = painterResource(R.drawable.ic_experiment),
              contentDescription = "Experimental",
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        ModelCountLabel(modelCount)
        if (task.shortDescription.isNotEmpty()) {
          Text(
            task.shortDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics {},
          )
        }
      }
    }
  }
}

@Composable
private fun NewBadge() {
  Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.customColors.newFeatureContainerColor) {
    Text(
      "New",
      color = MaterialTheme.customColors.newFeatureTextColor,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    )
  }
}

@Composable
private fun ModelCountLabel(count: Int) {
  val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
  AnimatedContent(
    targetState = count,
    transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
    label = "model count",
  ) { value ->
    Text(
      if (value == 1) "1 Model" else "$value Models",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.clearAndSetSemantics {},
    )
  }
}
```

- [ ] **Step 6: `HomeTopBar.kt`**

```kotlin
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
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.ui.common.buildTrackableUrlAnnotatedString

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
    title = { Text(stringResource(R.string.app_name)) },
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
```

- [ ] **Step 7: `HomeScaffold.kt`**

```kotlin
package com.safecircle.aiplayground.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.safecircle.aiplayground.data.CategoryInfo
import com.safecircle.aiplayground.data.Task
import kotlinx.coroutines.launch

private val MAX_CONTENT_WIDTH = 840.dp

@Composable
fun HomeScaffold(
  sortedCategories: List<CategoryInfo>,
  tasksByCategory: Map<String, List<Task>>,
  enableAnimation: Boolean,
  sharedBadges: Boolean,
  onTaskClick: (Task) -> Unit,
  onMenuClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  val pagerState = rememberPagerState(pageCount = { sortedCategories.size })
  val scope = rememberCoroutineScope()

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    topBar = { HomeTopBar(scrollBehavior = scrollBehavior, onMenuClick = onMenuClick) },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(modifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxWidth()) {
        if (sortedCategories.size > 1) {
          CategoryChips(
            categories = sortedCategories,
            selectedIndex = pagerState.currentPage,
            onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
          )
        }
        TaskList(
          pagerState = pagerState,
          sortedCategories = sortedCategories,
          tasksByCategory = tasksByCategory,
          enableAnimation = enableAnimation,
          sharedBadges = sharedBadges,
          onTaskClick = onTaskClick,
        )
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
      }
    }
  }
}
```

- [ ] **Step 8: `HomeDrawer.kt`**

```kotlin
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
```

- [ ] **Step 9: Rewrite `HomeScreen.kt`**

Keep the license header. Replace the entire file contents after it with:

```kotlin
package com.safecircle.aiplayground.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.safecircle.aiplayground.R
import com.safecircle.aiplayground.data.CategoryInfo
import com.safecircle.aiplayground.data.Task
import com.safecircle.aiplayground.ui.common.tos.AppTosDialog
import com.safecircle.aiplayground.ui.common.tos.TosViewModel
import com.safecircle.aiplayground.ui.modelmanager.ModelManagerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LOADING_DEBOUNCE_MS = 200L
private const val NOTIFICATION_PERMISSION_DELAY_MS = 2000L
private const val DRAWER_NAV_DELAY_MS = 50L

@Composable
fun HomeScreen(
  modelManagerViewModel: ModelManagerViewModel,
  tosViewModel: TosViewModel,
  navigateToTaskScreen: (Task) -> Unit,
  onModelsClicked: () -> Unit,
  enableAnimation: Boolean,
  modifier: Modifier = Modifier,
) {
  val uiState by modelManagerViewModel.uiState.collectAsState()
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showTosDialog by remember { mutableStateOf(!tosViewModel.getIsTosAccepted()) }
  val context = LocalContext.current

  val sortedCategories =
    remember(uiState.tasks) {
      sortCategories(uiState.tasks.map { it.category }.distinctBy { it.id }) {
        getCategoryLabel(context, it)
      }
    }

  if (!showTosDialog) {
    // Only show the spinner if loading takes longer than the debounce, to avoid a flicker.
    val showSpinner = rememberDebouncedLoading(uiState.loadingModelAllowlist)
    when {
      showSpinner -> HomeLoading()
      !uiState.loadingModelAllowlist ->
        HomeContent(
          sortedCategories = sortedCategories,
          tasksByCategory = uiState.tasksByCategory,
          enableAnimation = enableAnimation,
          navigateToTaskScreen = navigateToTaskScreen,
          onModelsClicked = onModelsClicked,
          onSettingsClicked = { showSettingsDialog = true },
          modifier = modifier,
        )
    }
  }

  if (showTosDialog) {
    AppTosDialog(
      onTosAccepted = {
        showTosDialog = false
        tosViewModel.acceptTos()
      }
    )
  }

  if (showSettingsDialog) {
    SettingsDialog(
      curThemeOverride = modelManagerViewModel.readThemeOverride(),
      modelManagerViewModel = modelManagerViewModel,
      onDismissed = { showSettingsDialog = false },
    )
  }

  if (uiState.loadingModelAllowlistError.isNotEmpty()) {
    AlertDialog(
      icon = {
        Icon(
          Icons.Rounded.Error,
          contentDescription = stringResource(R.string.cd_error),
          tint = MaterialTheme.colorScheme.error,
        )
      },
      title = { Text(uiState.loadingModelAllowlistError) },
      text = { Text("Please check your internet connection and try again later.") },
      onDismissRequest = { modelManagerViewModel.loadModelAllowlist() },
      confirmButton = {
        TextButton(onClick = { modelManagerViewModel.loadModelAllowlist() }) { Text("Retry") }
      },
      dismissButton = {
        TextButton(onClick = { modelManagerViewModel.clearLoadModelAllowlistError() }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun HomeContent(
  sortedCategories: List<CategoryInfo>,
  tasksByCategory: Map<String, List<Task>>,
  enableAnimation: Boolean,
  navigateToTaskScreen: (Task) -> Unit,
  onModelsClicked: () -> Unit,
  onSettingsClicked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  NotificationPermissionEffect()
  // Close the drawer when back is pressed.
  BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      HomeDrawerContent(
        onModelsClick = {
          scope.launch {
            drawerState.close()
            delay(DRAWER_NAV_DELAY_MS)
            onModelsClicked()
          }
        },
        onSettingsClick = {
          onSettingsClicked()
          scope.launch { drawerState.close() }
        },
      )
    },
    gesturesEnabled = drawerState.isOpen,
    modifier = modifier,
  ) {
    HomeScaffold(
      sortedCategories = sortedCategories,
      tasksByCategory = tasksByCategory,
      enableAnimation = enableAnimation,
      sharedBadges = true,
      onTaskClick = navigateToTaskScreen,
      onMenuClick = {
        scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() }
      },
    )
  }
}

@Composable
private fun rememberDebouncedLoading(loading: Boolean): Boolean {
  var delayed by remember { mutableStateOf(false) }
  LaunchedEffect(loading) {
    if (loading) {
      delay(LOADING_DEBOUNCE_MS)
      delayed = true
    } else {
      delayed = false
    }
  }
  return delayed
}

@Composable
private fun HomeLoading() {
  Row(
    modifier = Modifier.fillMaxSize(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    LoadingIndicator(modifier = Modifier.padding(end = 12.dp))
    Text(stringResource(R.string.loading_model_list), style = MaterialTheme.typography.bodyMedium)
  }
}

/** Asks for the notification permission shortly after the home screen is shown (API 33+). */
@Composable
private fun NotificationPermissionEffect() {
  val context = LocalContext.current
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
  LaunchedEffect(Unit) {
    delay(NOTIFICATION_PERMISSION_DELAY_MS)
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
}
```

- [ ] **Step 10: Update the call site, delete dead code, trim `CustomColors`**

In `GalleryNavGraph.kt` change the `HomeScreen(...)` call: remove the `onNotificationsClicked = ...` and `gm4 = false,` lines. (`ROUTE_NOTIFICATIONS` and its `composable` stay.)

Delete `ui/home/SquareDrawerItem.kt`.

Confirm the fields are unused, then delete them:

```bash
cd app/src/main/java/com/safecircle/aiplayground
grep -rnE 'appTitleGradientColors|tabHeaderBgColor|bgStarColor|homeBottomGradient|taskIconShapeBgColor|homePageTitleStyle|SquareDrawerItem' . | grep -v 'ui/theme/'
```

Expected: no output. Then remove `appTitleGradientColors`, `tabHeaderBgColor`, `taskIconShapeBgColor`, `homeBottomGradient`, `bgStarColor` from the `CustomColors` class and from both `lightCustomColors`/`darkCustomColors` in `Theme.kt`, and `homePageTitleStyle` from `Type.kt`.

- [ ] **Step 11: Build, test, look at it**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest 2>&1 | tail -30
./gradlew :app:installDebug 2>&1 | tail -3
adb shell monkey -p com.safecircle.aiplayground -c android.intent.category.LAUNCHER 1; sleep 4
adb exec-out screencap -p > /tmp/task4-home.png
```

Expected: build + 5 unit tests pass. In `/tmp/task4-home.png`: large "SafeCircle AI Playground" title with the intro under it, filter chips (if >1 category), 4 task cards each with a morphing-shape badge and a model-count label. Tap the menu button: drawer opens with Models and Settings; tap a card: model list opens (navigation still works). `adb logcat -d | grep FATAL` is empty.

- [ ] **Step 12: Commit**

```bash
git add -A app/src
git commit -m "feat: redesign home with collapsing app bar, expressive cards and chips

Splits HomeScreen.kt into focused files and removes the dead gm4 variant.

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Motion-scheme transitions, shared element, predictive back

**Files:**
- Create: `app/src/main/java/com/safecircle/aiplayground/ui/navigation/NavTransitions.kt`
- Modify: `app/src/main/java/com/safecircle/aiplayground/ui/navigation/GalleryNavGraph.kt`
- Modify: `app/src/main/java/com/safecircle/aiplayground/GalleryAppTopBar.kt`
- Modify: `app/src/main/java/com/safecircle/aiplayground/ui/modelmanager/ModelManager.kt`

**Interfaces:**
- Consumes: `LocalSharedTransitionScope`, `LocalAnimatedVisibilityScope`, `sharedBadge` (Task 3).
- Produces: `internal class NavTransitions` with `slideEnter/slideExit/slideUpEnter/slideDownExit(scope: AnimatedContentTransitionScope<*>)`, `@Composable internal fun rememberNavTransitions(): NavTransitions`; `GalleryTopAppBar(..., titleIcon: (@Composable () -> Unit)? = null)`.

- [ ] **Step 1: `NavTransitions.kt`**

```kotlin
package com.safecircle.aiplayground.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset

/** Navigation transitions driven by the theme's [androidx.compose.material3.MotionScheme]. */
internal class NavTransitions(
  private val offsetSpec: FiniteAnimationSpec<IntOffset>,
  private val fadeSpec: FiniteAnimationSpec<Float>,
) {
  fun slideEnter(scope: AnimatedContentTransitionScope<*>): EnterTransition =
    scope.slideIntoContainer(
      towards = AnimatedContentTransitionScope.SlideDirection.Left,
      animationSpec = offsetSpec,
    ) + fadeIn(fadeSpec)

  fun slideExit(scope: AnimatedContentTransitionScope<*>): ExitTransition =
    scope.slideOutOfContainer(
      towards = AnimatedContentTransitionScope.SlideDirection.Right,
      animationSpec = offsetSpec,
    ) + fadeOut(fadeSpec)

  fun slideUpEnter(scope: AnimatedContentTransitionScope<*>): EnterTransition =
    scope.slideIntoContainer(
      towards = AnimatedContentTransitionScope.SlideDirection.Up,
      animationSpec = offsetSpec,
    ) + fadeIn(fadeSpec)

  fun slideDownExit(scope: AnimatedContentTransitionScope<*>): ExitTransition =
    scope.slideOutOfContainer(
      towards = AnimatedContentTransitionScope.SlideDirection.Down,
      animationSpec = offsetSpec,
    ) + fadeOut(fadeSpec)
}

@Composable
internal fun rememberNavTransitions(): NavTransitions {
  val motion = MaterialTheme.motionScheme
  return remember(motion) { NavTransitions(motion.defaultSpatialSpec(), motion.defaultEffectsSpec()) }
}
```

- [ ] **Step 2: Use it in `GalleryNavGraph.kt`**

Delete the constants `ENTER_ANIMATION_DURATION_MS`, `ENTER_ANIMATION_EASING`, `ENTER_ANIMATION_DELAY_MS`, `EXIT_ANIMATION_DURATION_MS`, `EXIT_ANIMATION_EASING`, the functions `enterTween`, `exitTween`, `slideEnter`, `slideExit`, `slideUpEnter`, `slideDownExit`, and now-unused imports (`EaseOutExpo`, `FastOutSlowInEasing`, `FiniteAnimationSpec`, `tween`, `IntOffset`, `AnimatedContentTransitionScope`, `EnterTransition`/`ExitTransition` stay because the NavHost defaults use them).

At the top of `GalleryNavHost` (after the `val modelManagerUiState ...` line) add `val nav = rememberNavTransitions()`. Then replace every call:
- `slideEnter()` -> `nav.slideEnter(this)`
- `slideExit()` -> `nav.slideExit(this)`
- `slideUpEnter()` -> `nav.slideUpEnter(this)`
- `slideDownExit()` -> `nav.slideDownExit(this)`

(All of them are inside `enterTransition = { ... }` / `exitTransition = { ... }` lambdas whose receiver is `AnimatedContentTransitionScope<NavBackStackEntry>`; `this` is correct there.) The existing conditional logic and defaults stay as they are.

- [ ] **Step 3: Shared transition layout + animated-visibility scope**

Wrap the `NavHost(...) { ... }` call (the whole block from `NavHost(` to its closing brace, before the `// Handle incoming intents` comment) in:

```kotlin
  SharedTransitionLayout {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
      NavHost( /* unchanged */ ) { /* unchanged */ }
    }
  }
```

In the `composable(route = ROUTE_HOMESCREEN) { ... }` block and the `composable(route = ROUTE_MODEL_LIST, ...) { ... }` block, wrap the existing body:

```kotlin
    composable(route = ROUTE_HOMESCREEN) {
      CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
        Box(modifier = modifier.fillMaxSize()) { /* HomeScreen(...) unchanged */ }
      }
    }
```

and the same `CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) { pickedTask?.let { ModelManager(...) } }` for the model list.

Add imports: `androidx.compose.animation.SharedTransitionLayout`, `androidx.compose.runtime.CompositionLocalProvider`, `com.safecircle.aiplayground.ui.common.expressive.LocalAnimatedVisibilityScope`, `com.safecircle.aiplayground.ui.common.expressive.LocalSharedTransitionScope`.

- [ ] **Step 4: `titleIcon` slot in `GalleryTopAppBar` and use it in `ModelManager`**

In `GalleryAppTopBar.kt` add the parameter `titleIcon: (@Composable () -> Unit)? = null,` after `subtitle`, and inside the title `Row`, before the existing `if (title == stringResource(R.string.app_name)) {` block, add:

```kotlin
          titleIcon?.invoke()
```

In `ModelManager.kt` change the `GalleryTopAppBar(` call to pass:

```kotlin
        titleIcon = { TaskIcon(task = task, modifier = Modifier.sharedBadge(task.id), width = 32.dp) },
```

with imports `androidx.compose.ui.unit.dp`, `com.safecircle.aiplayground.ui.common.TaskIcon`, `com.safecircle.aiplayground.ui.common.expressive.sharedBadge`.

- [ ] **Step 5: Build and check on the emulator**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -30
./gradlew :app:installDebug 2>&1 | tail -3
adb shell monkey -p com.safecircle.aiplayground -c android.intent.category.LAUNCHER 1
```

Manually verify, then record the result:
1. Tap a task card: the badge visibly flies into the model-list header and the list slides in with a springy settle.
2. Back button and the edge-swipe back gesture: the list slides out; with "Predictive back animations" enabled (Developer options), dragging the gesture previews the previous screen; releasing completes it.
3. Models (drawer) -> slides up; back -> slides down. Model page -> benchmark still works.
4. `adb logcat -d | grep -E "FATAL|IllegalArgument"` is empty (duplicate shared keys would show here).

If the shared element misbehaves with predictive back (flicker, stuck overlay) and cannot be fixed quickly: remove only the `sharedBadge(...)` modifier usages and the `SharedTransitionLayout` wrapper, keep the springs, and note it in the final report.

- [ ] **Step 6: Commit**

```bash
git add -A app/src
git commit -m "feat: spring-based navigation transitions and shared task badge

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Adaptive layout (rail >=600dp, list-detail >=840dp)

**Files:**
- Create: `ui/home/HomeLayout.kt`, `ui/home/HomeRail.kt`, `ui/home/HomeListDetail.kt`
- Modify: `ui/home/HomeScreen.kt`, `ui/navigation/GalleryNavGraph.kt`, `ui/modelmanager/ModelManager.kt`
- Modify: `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/safecircle/aiplayground/ui/home/HomeLayoutTest.kt`

(Paths under `app/src/main/java/com/safecircle/aiplayground/` when relative.)

**Interfaces:**
- Consumes: `HomeScaffold`, `HomeDrawerContent` (Task 4), `ModelManager` (Task 5 version).
- Produces:
  - `enum class HomeLayout { COMPACT, MEDIUM, EXPANDED }` with `companion fun forWidthDp(widthDp: Int): HomeLayout`; `@Composable fun rememberHomeLayout(): HomeLayout`
  - `HomeScreen(..., layout: HomeLayout = HomeLayout.COMPACT, detailPane: (@Composable () -> Unit)? = null)`
  - `ModelManager(..., embedded: Boolean = false)`

- [ ] **Step 1: Write the failing test `HomeLayoutTest.kt`**

```kotlin
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
```

- [ ] **Step 2: Run it and confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*HomeLayoutTest' 2>&1 | tail -20` -> FAIL (unresolved `HomeLayout`).

- [ ] **Step 3: `HomeLayout.kt`**

```kotlin
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
```

Run the Step 2 command -> PASS.

- [ ] **Step 4: Strings**

Add to `res/values/strings.xml` next to the other `drawer_*` strings:

```xml
  <string name="home_label" translatable="true">Home</string>
  <string name="home_select_task_hint" translatable="true">Select a use case to see its models</string>
```

- [ ] **Step 5: `HomeRail.kt`**

```kotlin
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
fun HomeRail(onModelsClick: () -> Unit, onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
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
```

- [ ] **Step 6: `HomeListDetail.kt`**

```kotlin
package com.safecircle.aiplayground.ui.home

import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val LIST_PANE_WIDTH = 420.dp

/** Two-pane home: use cases on the left, the selected use case's models on the right. */
@Composable
fun HomeListDetail(
  list: @Composable () -> Unit,
  detail: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  // The navigator supplies the window-aware directive/value (hinges, partitions).
  val navigator = rememberListDetailPaneScaffoldNavigator<Unit>()
  ListDetailPaneScaffold(
    directive = navigator.scaffoldDirective,
    value = navigator.scaffoldValue,
    modifier = modifier,
    listPane = { AnimatedPane(Modifier.preferredWidth(LIST_PANE_WIDTH)) { list() } },
    detailPane = { AnimatedPane { detail() } },
  )
}
```

(`preferredWidth` is a member extension of the pane scope, so it needs no import.)

- [ ] **Step 7: `HomeScreen` takes a layout**

In `HomeScreen.kt` add parameters to `HomeScreen` and `HomeContent`: `layout: HomeLayout = HomeLayout.COMPACT` and `detailPane: (@Composable () -> Unit)? = null` (pass them through). Replace the body of `HomeContent` from `val scope = ...` onward with:

```kotlin
  val scope = rememberCoroutineScope()
  NotificationPermissionEffect()

  fun scaffold(onMenuClick: (() -> Unit)?, sharedBadges: Boolean, scaffoldModifier: Modifier = Modifier) =
    @Composable {
      HomeScaffold(
        sortedCategories = sortedCategories,
        tasksByCategory = tasksByCategory,
        enableAnimation = enableAnimation,
        sharedBadges = sharedBadges,
        onTaskClick = navigateToTaskScreen,
        onMenuClick = onMenuClick,
        modifier = scaffoldModifier,
      )
    }

  if (layout == HomeLayout.COMPACT) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // Close the drawer when back is pressed.
    BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }
    ModalNavigationDrawer(
      drawerState = drawerState,
      drawerContent = {
        HomeDrawerContent(
          onModelsClick = {
            scope.launch {
              drawerState.close()
              delay(DRAWER_NAV_DELAY_MS)
              onModelsClicked()
            }
          },
          onSettingsClick = {
            onSettingsClicked()
            scope.launch { drawerState.close() }
          },
        )
      },
      gesturesEnabled = drawerState.isOpen,
      modifier = modifier,
    ) {
      scaffold(
        onMenuClick = {
          scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() }
        },
        sharedBadges = true,
      )()
    }
  } else {
    Row(modifier = modifier.fillMaxSize()) {
      HomeRail(onModelsClick = onModelsClicked, onSettingsClick = onSettingsClicked)
      if (layout == HomeLayout.EXPANDED && detailPane != null) {
        // Both panes are on screen, so the shared-element key would be used twice: disable it.
        HomeListDetail(
          list = scaffold(onMenuClick = null, sharedBadges = false),
          detail = detailPane,
          modifier = Modifier.weight(1f),
        )
      } else {
        scaffold(onMenuClick = null, sharedBadges = true, scaffoldModifier = Modifier.weight(1f))()
      }
    }
  }
```

Add imports `androidx.compose.foundation.layout.Row` (already there), `androidx.compose.foundation.layout.fillMaxSize` (already there).

- [ ] **Step 8: `ModelManager` embedded mode**

In `ModelManager.kt` add parameter `embedded: Boolean = false` (after `onBenchmarkClicked`), then:
- `BackHandler { navigateUp() }` -> `BackHandler(enabled = !embedded) { navigateUp() }`
- In the `GalleryTopAppBar(` call: `leftAction = if (embedded) null else AppBarAction(actionType = AppBarActionType.NAVIGATE_UP, actionFn = navigateUp),` and `titleIcon = if (embedded) null else { { TaskIcon(task = task, modifier = Modifier.sharedBadge(task.id), width = 32.dp) } },`

(`modelCount == 0 -> navigateUp()` stays; in embedded mode the caller's `navigateUp` clears the selection.)

- [ ] **Step 9: Nav graph: layout-aware home route**

In the `composable(route = ROUTE_HOMESCREEN)` block (Task 5 version) compute `val layout = rememberHomeLayout()` inside the `CompositionLocalProvider`, and change the `HomeScreen(...)` call:

```kotlin
          navigateToTaskScreen = { task ->
            pickedTask = task
            enableModelListAnimation = true
            // In the two-pane layout the models show beside the list; otherwise navigate.
            if (layout != HomeLayout.EXPANDED) navController.navigate(ROUTE_MODEL_LIST)
            logEvent(GalleryEvent.CAPABILITY_SELECT, mapOf("capability_name" to task.id))
          },
          onModelsClicked = { navController.navigate(ROUTE_MODEL_MANAGER) },
          layout = layout,
          detailPane = {
            val task = pickedTask
            if (task == null) {
              HomeDetailPlaceholder()
            } else {
              ModelManager(
                viewModel = modelManagerViewModel,
                task = task,
                enableAnimation = false,
                embedded = true,
                onModelClicked = { model -> navController.navigate("$ROUTE_MODEL/${task.id}/${model.name}") },
                onBenchmarkClicked = { model -> navController.navigate("$ROUTE_BENCHMARK/${model.name}") },
                navigateUp = { pickedTask = null },
              )
            }
          },
```

Add the placeholder in `HomeListDetail.kt`:

```kotlin
@Composable
fun HomeDetailPlaceholder() {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(
      stringResource(R.string.home_select_task_hint),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
```

(imports: `Box`, `fillMaxSize`, `Alignment`, `MaterialTheme`, `Text`, `stringResource`, `R`.) Import `HomeLayout`, `rememberHomeLayout`, `HomeDetailPlaceholder` in the nav graph.

- [ ] **Step 10: Manifest: do not recreate on resize/fold/rotate**

In `AndroidManifest.xml`, on `MainActivity` replace `android:configChanges="uiMode"` with:

```xml
            android:configChanges="uiMode|orientation|screenSize|smallestScreenSize|screenLayout|density"
```

and update the comment above it to say the activity is also not recreated on resize/fold so selection and navigation state survive. (`MainActivity` discards saved state on creation, so a recreation would otherwise dump users back on Home.)

- [ ] **Step 11: Build, test, verify on a tablet-sized window**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest 2>&1 | tail -30
./gradlew :app:installDebug 2>&1 | tail -3
adb shell wm size 1600x2560 && adb shell wm density 320   # 800dp wide -> MEDIUM
adb shell monkey -p com.safecircle.aiplayground -c android.intent.category.LAUNCHER 1; sleep 4
adb exec-out screencap -p > /tmp/task6-medium.png
adb shell wm size 2560x1600 && adb shell wm density 240   # ~1280dp wide -> EXPANDED
sleep 2; adb exec-out screencap -p > /tmp/task6-expanded-empty.png
# tap the first task card, then:
adb exec-out screencap -p > /tmp/task6-expanded-selected.png
adb shell wm size reset && adb shell wm density reset
```

Expected: MEDIUM shows a rail on the left and a single column of cards (no drawer button); EXPANDED shows the rail, the card list on the left and "Select a use case..." on the right; tapping a card fills the right pane with that task's models without navigating; tapping a model opens it full screen; back returns to the two-pane home with the selection kept. Resetting `wm` returns to the phone layout with the drawer. No `FATAL` in logcat.

- [ ] **Step 12: Commit**

```bash
git add -A app/src
git commit -m "feat: adaptive home with navigation rail and list-detail panes

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Final verification and review

**Files:** none (fixes only if something fails).

- [ ] **Step 1: Full automated pass**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest 2>&1 | tail -30
./gradlew :app:connectedDebugAndroidTest 2>&1 | tail -30
```

Expected: build OK; unit tests `CustomColorsTest`, `CategoryOrderTest`, `HomeLayoutTest`, plus the existing `VigilClassifierTest` and `ExampleUnitTest` pass; instrumented `ExpressiveCardTest` passes. Report the real counts. If `ExampleInstrumentedTest` already fails before this work (check with `git stash` / `git log`), say so rather than hiding it.

- [ ] **Step 2: Visual matrix (screenshots to `/tmp/final/`)**

```bash
mkdir -p /tmp/final
adb shell cmd uimode night no;  # light
adb exec-out screencap -p > /tmp/final/home-light.png
adb shell cmd uimode night yes; # dark
adb exec-out screencap -p > /tmp/final/home-dark.png
adb shell cmd uimode night no
```

Also switch to AMOLED via Settings in the app and capture `home-amoled.png`. Open each PNG and look: the badge colours must follow the wallpaper colour scheme (change the wallpaper accent or compare with a static-scheme device if available), text must be legible on every card, and the out-of-scope screens (chat, Prompt Lab, Safety Detection, Models page) must still render sensibly with the dp shape scale and derived palette.

- [ ] **Step 3: Review-focus checks**

1. Reduced motion: `adb shell settings put global animator_duration_scale 0` -> relaunch; cards are visible, navigation completes. Reset with `... 1`.
2. Large font: `adb shell settings put system font_scale 2.0` -> the large app bar wraps its title and subtitle without clipping or overlapping the chips. Reset with `... 1.0`.
3. Resize with a selection: in EXPANDED (`wm size 2560x1600; wm density 240`) select a task, then `wm size 1080x2400; wm density 420` -> app does not crash and is not recreated; Home shows the phone layout; tapping the card navigates. Then `wm size reset; wm density reset`.
4. Dynamic colour off path: confirm `lightColorScheme/darkColorScheme` fallback is covered by `CustomColorsTest` (already run).

- [ ] **Step 4: Code review**

Run the `code-review` skill (`/code-review`) on the branch diff, fix any CRITICAL/HIGH findings, re-run Step 1.

- [ ] **Step 5: Report**

Summarise to the user: what changed, test results with real numbers, anything skipped or deviated (see the spec's "Plan-time adjustments"), and the alpha-dependency decision. Do not push, tag, bump the version, or open a PR unless asked.
