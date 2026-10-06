# Material 3 Expressive: Foundation + Shell

Date: 2026-10-06
Status: awaiting review

## Goal

Redesign the app's look and feel with Material 3 Expressive: expressive shapes and
typography, spring-based motion, meaningful transitions, and adaptive layout for
tablets and foldables. Existing behavior (model download, chat, classification,
benchmark, Vigil/Horizon content) does not change.

This is sub-project 1+2 of five. Later cycles, each with their own spec and plan:
model manager/list, chat surfaces, remaining screens and dialogs.

## Decisions (agreed with user)

- Scope: foundation (design system) + shell (home, navigation, adaptive) first.
- Color: dynamic color on Android 12+, static SafeCircle-blue tonal scheme below.
  AMOLED override stays.
- Shell: polished phone flow (current navigation kept) + adaptive layout at 600dp+.
- Compose-only; only AndroidX artifacts added (managed by the Compose BOM
  `2026.09.00`). No third-party UI libraries.

## 1. Foundation

Location: `ui/theme/` plus new `ui/common/expressive/`.

- `GalleryTheme` uses `MaterialExpressiveTheme` with `MotionScheme.expressive()`.
  Color selection logic unchanged: dynamic on API 31+, otherwise a static scheme
  seeded from `#3174F1`; AMOLED override retained.
- `CustomColors` shrinks. Hardcoded red/green/blue/yellow task palettes are removed;
  each task takes a container-role pair (`primaryContainer`, `secondaryContainer`,
  `tertiaryContainer`, `errorContainer`). Only semantic colors remain (success,
  warning, chat bubbles). All current usages are migrated in the same step.
- Shapes: expressive scale (large-increased 20dp, extra-large-increased 32dp,
  extra-extra-large 48dp). Task icon badges use `MaterialShapes` polygons that morph
  on press.
- Typography: M3 scale plus emphasized variants for titles and hero text.
- Motion: one `LocalMotion` helper exposing the scheme's spatial and effects springs.
  All animation reads from it; no ad-hoc `tween` in touched files. Animator duration
  scale 0 yields instant transitions (reduced motion).
- Shared components: `ExpressiveCard` (spring press scale + shape morph),
  `ExpressiveTopBar` (`LargeFlexibleTopAppBar`), `LoadingIndicator` replacing
  `GlitteringShapesLoader`/`RotationalLoader` where used by touched screens, and
  connected button groups for segmented choices.

## 2. Shell

### Home (compact)
- Large title and intro text collapse into a `LargeFlexibleTopAppBar` (big at rest,
  shrinks on scroll; LiteRT link in the subtitle).
- Task cards become an `ExpressiveCard` list with staggered spring entrance, a
  morphing icon badge colored from theme roles, and a model-count chip.
- Vigil/Horizon and new-release banners animate with `AnimatedVisibility` springs
  and stay dismissible.
- Drawer retained, restyled with expressive selected-item pills.
- `HomeScreen.kt` (1171 lines) is split into focused files (header, task list, task
  card, banners), each under ~400 lines, behavior unchanged.

### Navigation (`GalleryNavGraph.kt`)
- Home -> model list -> model: shared-axis transition from the motion scheme's
  spatial spring; the ad-hoc `slideEnter`/`slideExit` helpers are removed.
- Notifications keeps its vertical slide on the same springs.
- Tapped task icon badge is a shared element into the next screen's header
  (`SharedTransitionLayout`). Fallback if incompatible with predictive back: drop
  the shared element, keep shared-axis.
- Predictive back on every route.

### Adaptive (>= 600dp)
- `NavigationRail` replaces the drawer; destinations: Home, Models, Notifications,
  Settings.
- Home and model list become list-detail panes (`ListDetailPaneScaffold`).
- Content max width ~840dp; cards use two columns at >= 840dp.
- Hinge awareness through the adaptive scaffold.

### Out of scope
Chat, Prompt Lab, Safety Detection, Benchmark and dialogs get no redesign here; they
inherit the new theme tokens only.

## 3. Testing, risks, rollout

Verification
- `./gradlew assembleDebug` and existing unit tests after each step.
- Emulator screenshots: light, dark, AMOLED, dynamic color; compact, expanded and
  fold-sized windows.
- Compose UI tests: task card navigation, rail replaces drawer at 600dp, banner
  dismiss.
- Reduced-motion check (animator scale 0).

Risks
- Expressive and adaptive APIs are experimental and may differ in the BOM version.
  First plan task is a compile spike on a minimal screen; substitute and report if
  something is missing.
- Shared elements + predictive back in `navigation-compose` 2.10 may misbehave
  (fallback above).
- Regression risk in `HomeScreen.kt` and `GalleryNavGraph.kt` (mitigated by small
  commits and the file split).
- `CustomColors` removal touches chat and benchmark screens; each reference is
  migrated to theme roles in the same commit.

Rollout
- Branch `feat/m3-expressive-foundation-shell`, small commits in order: theme and
  tokens, shared components, home, transitions, adaptive layout.
- No release or version bump unless requested.

## Plan-time adjustments (2026-10-06)

Found while checking the spec against the code and the resolved dependencies. These
override the text above.

- **Material3 version.** The project resolves `material3` to `1.4.0` (BOM pin). That
  version has no `MaterialShapes`, `LoadingIndicator`, `ButtonGroup`, `ToggleButton` or
  public `MotionScheme.expressive()`. The plan moves `material3` to `1.5.0-alpha27`
  (already in the Gradle cache). This is an alpha dependency in a shipped app; Task 1 is
  a go/no-go compile spike. Fallback is 1.4.0 with no shape presets or loading
  indicator, which needs a re-plan.
- **Typeface.** The app uses Nunito, not Roboto Flex. It stays.
- **Shape scale bug.** Existing `RoundedCornerShape(4)` etc. are percent corners.
  They become dp corners app-wide, so out-of-scope screens change slightly.
- **Dropped (no consumer in this cycle):** `LocalMotion` helper (use
  `MaterialTheme.motionScheme`), `ButtonGroup`/connected buttons, `ExpressiveTopBar`
  wrapper (home uses `LargeFlexibleTopAppBar` directly), icon press-morph (spring press
  scale instead), two-column card grid, drawer item descriptions.
- **Banners are not on Home.** The Vigil banner lives on the Models page (next cycle);
  `NewReleaseNotification` is unused. Nothing to animate on Home for this.
- **Rail destinations:** Home, Models, Settings. Notifications has no entry point today
  (`onNotificationsClicked` was never used), so it is not added.
- **Breakpoints:** rail at >= 600dp (medium); list-detail at >= 840dp (expanded), per
  Material window size classes. The rail appears on the Home route only.
- **Dead code removed:** the `gm4` home variant (only ever called with `false`),
  `SquareDrawerItem`, unused `CustomColors` fields.
- **`configChanges`** extended on `MainActivity` so resize/fold/rotate does not recreate
  the activity (it discards saved state and would dump users back on Home).
