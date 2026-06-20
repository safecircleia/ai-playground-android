<div align="center">

<img src="docs/icon.svg" width="120" alt="SafeCircle AI Playground icon" />

# SafeCircle AI Playground

**Run on-device AI models directly on Android — no cloud, no data leaving your device.**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-30%20(Android%2011)-brightgreen)](https://developer.android.com/about/versions/11)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Version](https://img.shields.io/badge/Version-1.0.0-orange)](https://github.com/safecircleia/ai-playground-android/releases)
[![Repo](https://img.shields.io/badge/GitHub-safecircleia%2Fai--playground--android-181717?logo=github)](https://github.com/safecircleia/ai-playground-android)

</div>

---

## Overview

SafeCircle AI Playground is an Android application for exploring, benchmarking, and interacting with on-device AI models powered by [Google AI Edge](https://ai.google.dev/edge). All inference runs locally — your data never leaves the device.

Built on top of the [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) open-source project, this fork adds SafeCircle branding, a Material You–compliant UI, and an AMOLED-first dark theme.

---

## Features

| Feature | Description |
|---|---|
| 💬 **LLM Chat** | Multi-turn conversations with on-device large language models |
| 📝 **LLM Single Turn** | Single prompt → response with template support |
| 🖼️ **Ask Image** | Multimodal image understanding and Q&A |
| 🎵 **Ask Audio** | Audio transcription and understanding |
| 🛡️ **Safety Detection** | On-device content safety classification |
| 🤖 **Agents** | Agentic workflows with skill support |
| 📊 **Benchmark** | Measure model latency and throughput on-device |
| 🔔 **Notifications** | Schedule and manage AI-powered reminders |
| 🎛️ **Model Manager** | Browse, download, and manage local models (Horizon, Horizon Edge, Experimental) |

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material Design 3
- **DI:** Hilt
- **Storage:** Proto DataStore
- **Inference:** Google AI Edge (LiteRT / MediaPipe)
- **Theme:** Material You dynamic color, AMOLED black mode
- **Min SDK:** 30 (Android 11) · **Target SDK:** 35 (Android 15)

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android device or emulator running Android 11+ (API 30+)
- For Material You dynamic theming: Android 12+ (API 31+)

### Build & Run

```bash
git clone https://github.com/safecircleia/ai-playground-android.git
cd ai-playground-android
```

Open in Android Studio and run on a connected device or emulator.

> **Note:** The app requires a physical or emulated device with sufficient RAM (4 GB+) to load larger models.

---

## Model Categories

The Model Manager organises models into three tiers:

| Category | Description |
|---|---|
| **Horizon** | Stable, production-ready models |
| **Horizon Edge** | Optimised edge variants for lower-end hardware |
| **Experimental** | Preview / research models — may be unstable |

Models are downloaded on demand and stored locally. No account required.

---

## Theming

The app supports four theme modes selectable from Settings:

| Mode | Behaviour |
|---|---|
| **Auto** | Follows system dark/light preference |
| **Light** | Always light |
| **Dark** | Always dark |
| **Black** | AMOLED true-black surfaces — preserves Material You accent colors |

On Android 12+ the app automatically picks up your wallpaper accent colors via Material You dynamic color.

---

## Project Structure

```
app/src/main/java/com/safecircle/aiplayground/
├── data/               # Models, tasks, config, repositories
├── di/                 # Hilt modules
├── notifications/      # Notification scheduling
├── runtime/            # LLM / AI Core inference helpers
├── ui/
│   ├── benchmark/      # Benchmark screen
│   ├── common/         # Shared composables (chat, model items, etc.)
│   ├── home/           # Home screen + settings dialog
│   ├── llmchat/        # Multi-turn chat
│   ├── llmsingleturn/  # Single-turn prompting
│   ├── modelmanager/   # Model browser + download manager
│   ├── notifications/  # Notifications screen
│   ├── safety/         # Safety detection screen
│   └── theme/          # Material 3 theme, colors, shapes, typography
└── worker/             # Background download worker
```

---

## Contributing

1. Fork the repo
2. Create a feature branch: `git checkout -b feat/my-feature`
3. Commit using [Conventional Commits](https://www.conventionalcommits.org): `feat: add X`
4. Open a pull request against `main`

---

## License

```
Copyright 2026 SafeCircle

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

Based on [google-ai-edge/gallery](https://github.com/google-ai-edge/gallery), originally Copyright 2025 Google LLC.

---

<div align="center">
  Made with ❤️ by <a href="https://safecircle.tech">SafeCircle</a>
</div>
