# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
