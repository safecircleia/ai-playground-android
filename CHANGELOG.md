# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.2] - 2026-10-04

### Changed
- Model Manager promo banner now announces Vigil instead of Horizon; it is shown again to users who dismissed the Horizon banner

## [1.1.1] - 2026-10-04

Replaces 1.1.0, which could not get past the "Loading model list…" screen.

### Fixed
- Crash on launch ("Loading model list…" never finishing) when more than one task sat outside the predefined task order, which Vigil's dedicated task triggered

### Changed
- Updated Kotlin to 2.4.20, Gradle to 9.8.0 and the Android Gradle Plugin to 9.4.1
- Updated all dependencies to their latest releases, including Compose BOM 2026.09.00, Hilt 2.60.1 (now on KSP instead of kapt), LiteRT-LM 0.17.1, MCP Kotlin SDK 0.15.0, Ktor 3.6.0 and Firebase BoM 34.19.0
- Raised `compileSdk` to 37 and `targetSdk` to 36

## [1.1.0] - 2026-10-04

### Added
- **Vigil** — compact multilingual encoder classifier that scores a whole conversation in a single pass for grooming, bullying, sexual content, isolation, personal-info requests, platform migration and threats. Each category is flagged at its own calibrated threshold; runs on CPU via LiteRT (about 1.3 GB, downloaded on demand)
- Dedicated **Vigil** task with its own model list, separate from Safety Detection and the chat modes, since Vigil does not generate text

### Changed
- Replaced the unused Play Services TFLite dependencies with LiteRT 2.2.0
- Pinned LiteRT-LM to 0.16.1 instead of `latest.release`, whose newer builds require Kotlin 2.4

## [1.0.0] - 2026-06-22

### Added
- Initial release of SafeCircle AI Playground for Android
- **Horizon Mobile INT4** — fastest on-device safety detection model (Gemma 3 1B, 4-bit, <600 MB)
- **Horizon Mobile INT8** — highest-accuracy mobile safety detection model (Gemma 3 1B, 8-bit)
- **Horizon Edge E2B** — general-purpose agent model fine-tuned from Gemma 4 E2B with 8 GB+ RAM support
- Safety Detection task with real-time conversation risk analysis (grooming, bullying, exploitation)
- LLM Chat task for general conversation with Horizon Edge models
- LLM Prompt Lab for single-turn prompting and experimentation
- **Agent Chat** — on-device agentic task execution with Skills and MCP tool support
  - 12 built-in skills: calculate-hash, create-calendar-event, interactive-map, kitchen-adventure, learn-something-new, mood-tracker, qr-code, query-wikipedia, read-calendar-events, schedule-notification, send-email, text-spinner
  - MCP server management (add/remove/toggle servers and tools)
  - Skill management UI (enable/disable individual skills)
  - Android intents: email, SMS, calendar events, notifications
  - JavaScript skill execution via embedded WebView
- GPU/CPU/NPU accelerator selection with automatic CPU fallback
- Speculative decoding support for compatible models
- Model import from local storage or URL
- HuggingFace OAuth authentication for gated model downloads
- Benchmark task for measuring model performance
- SafeCircle branding with Horizon AI persona
- Privacy-first: all inference runs entirely on-device, no data sent to servers
