<p align="center">
  <h1 align="center">IsekaiKuroshin</h1>
  <p align="center">On-Device AI Personal Growth & Gamification Platform</p>
  <p align="center">A fully local, MediaPipe-powered AI assistant that fuses health tracking, language learning, and drone control into an RPG narrative.</p>
</p>

<p align="center">
  <img alt="Status" src="https://img.shields.io/badge/status-Public-brightgreen">
  <img alt="Language" src="https://img.shields.io/badge/Kotlin-111.7K%20LOC-7F52FF">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android%2026%2B-3DDC84">
  <img alt="AI" src="https://img.shields.io/badge/AI-MediaPipe%20LlmInference-FF6F00">
  <img alt="UI" src="https://img.shields.io/badge/UI-37%2B%20Jetpack%20Compose%20screens-42A5F5">
  <img alt="Hardware" src="https://img.shields.io/badge/Hardware-ESP32%20%2B%20KiCad%20PCB-1FAEC8">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-blue">
</p>

---

## Overview

**IsekaiKuroshin** is an Android application that turns daily life into an isekai-style RPG —
powered entirely by **on-device AI** (no cloud, no API keys required for the core experience).

An AI Game Master narrates a personalized story via **BM25-based RAG retrieval** from embedded
novels. Real-world actions — exercise reps (MediaPipe Pose), hand seal gestures (MediaPipe
Hands), language learning progress, health metrics (Health Connect) — feed back into the
narrative and character progression. An optional custom **ESP32 wrist-controller** enables
physical drone control via BLE, completing the "fantasy meets reality" loop.

The entire system runs on a mid-range phone (Samsung Galaxy A34, 6 GB RAM). No server, no
cloud, no monthly bill.

## Key Features

### On-Device AI
- **MediaPipe LlmInference** — runs a local LLM (Gemma 3 1B INT4) directly on the phone
- **AI Game Master Engine** — generates context-aware story responses using BM25 RAG
  retrieval from chunked novel texts (e.g., Count of Monte Cristo)
- **Umbros AI Companion** — personality system with mood, relationship level, and
  conversation memory that evolves over time
- **Auto-fallback** — if the local model fails, gracefully falls back to a user-configured
  Gemini API key (optional, not required)

### Gamified Real-World Integration
- **Health Hub** — Health Connect API integration (weight tracking, fitness metrics);
  in-game rewards tied to real exercise
- **Exercise Recognition** — MediaPipe Pose counts push-ups, sit-ups, and rope-skipping
  reps in real-time via on-device pose estimation
- **Language Learning Engine** — AI-driven language tutor (EN/TR) with progress tracking
  that unlocks in-game abilities
- **Seal Practice** — MediaPipe Hands landmark recognition + Procrustes analysis for
  gesture-based "ancient seal" spell casting

### ESP32 Drone Controller ("Kolluk")
- Custom-designed **ESP32 wrist-controller** with MPU6050 gyroscope, ELRS TX module
  (E28/SX1280 2.4 GHz), and CRSF protocol
- BLE connection from the app; roll/pitch from wrist motion, throttle from potentiometer
- Full hardware design files included: KiCad PCB, EasyEDA project, Fritzing schematics,
  3D-printable enclosure, and production firmware
- See [`hardware/README.md`](hardware/README.md) for details

### Game Systems
- **Reality Engine** (Phase 8.2) — every action has potential and cost; LUCK stat
  protects during "fate moments"
- **Karma System** — axis-based attribute system (Violence↔Mercy, Chaos↔Order, etc.)
- **Dynamic World** — factions, settlements, and NPCs stored in Room database
- **Spell Studio** — craft and test spells with particle effects
- **Combat System** — turn-based with AI-narrated encounters
- **Billing** — Google Play billing integration (optional)

### 37+ Jetpack Compose Screens
Character status, inventory, quests, journal, map, health hub, drone controller, spell
studio, skill tree, seal practice, cultivation, crafting, settings, dashboard, and more.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin (111,700+ LOC) |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| AI / ML | MediaPipe LlmInference, MediaPipe Pose, MediaPipe Hands |
| DI | Hilt (KSP) |
| Database | Room (KSP), with DocumentChunk RAG storage |
| Media | Media3 ExoPlayer |
| Health | Android Health Connect |
| Auth | Firebase Auth |
| Notifications | AlarmScheduler, WorkManager |
| Hardware | ESP32 (Arduino C++), BLE, CRSF, MPU6050, ELRS |
| Build | Gradle 8.4, AGP 8.4, Kotlin 1.9.23 |
| Target | Android 8.0+ (API 26), tested on Samsung Galaxy A34 |

## Project Structure

```
IsekaiKuroshin/
├── app/
│   ├── src/main/java/com/example/isekaikuroshin/
│   │   ├── ai/              # GlobalAIManager, MemoryManager, AiNarrator
│   │   ├── api/             # Weather API
│   │   ├── auth/            # AuthManager
│   │   ├── billing/         # Google Play BillingManager
│   │   ├── ble/             # BLE Manager, DroneCommands, TelemetryParser
│   │   ├── combat/          # Combat state and logic
│   │   ├── data/            # GameState, PersistentDataManager, Room DB
│   │   ├── di/              # Hilt modules
│   │   ├── engine/          # GameMasterEngine, GestureRecognition, Pose, Voice
│   │   ├── game/            # GameStateManager
│   │   ├── initializer/     # App startup
│   │   ├── mapper/          # Data mapping
│   │   ├── models/          # MediaTag and domain models
│   │   ├── sound/           # Audio engine
│   │   ├── ui/              # 37+ Compose screens (adventure, combat, health, ...)
│   │   ├── utils/           # GameLogger, BM25Scorer, PromptManager, etc.
│   │   └── workers/         # DocumentProcessingWorker
│   └── src/main/assets/
│       ├── stories/              # RAG source texts (public domain novels)
│       └── hand_landmarker.task  # MediaPipe Hands model
├── hardware/                    # ESP32 wrist-controller design files
│   ├── kicad/                   # PCB layout & schematic
│   ├── easyeda/                 # EasyEDA project
│   ├── arduino/                 # Test sketches (MPU6050, E28 SPI, switch)
│   ├── firmware/                # Production ESP32 firmware (BLE + CRSF + ELRS)
│   ├── fritzing/                # Breadboard schematics
│   ├── elrs_config/             # ExpressLRS configuration
│   └── prints/                  # 3D print G-code
├── docs/                        # Technical analysis and design documents
├── tools/                       # Developer utilities
│   ├── kuroshin_insight_dashboard/  # Streamlit codebase analyzer
│   └── gorsel_etiketleyici/         # Axis-based media labeling tool
└── build.gradle.kts
```

## Installation

### Prerequisites

- **Android Studio** (Hedgehog 2023.1.1 or newer)
- **JDK 11+** (Android Studio bundles one; Gradle auto-detects)
- **Android device** with API 26+ (Android 8.0+) — tested on Samsung Galaxy A34 5G
- (Optional) ESP32 DevKit V1 + MPU6050 + E28 module for hardware features

### Setup

```bash
git clone https://github.com/KuroShinHQ/IsekaiKuroshin.git
cd IsekaiKuroshin
```

1. Open the project in **Android Studio**
2. Let Gradle sync — it will download all dependencies automatically
3. (Optional) Configure your Gemini API key in **Settings → AI Settings**
   inside the app, or use the on-device local model (Gemma 3 1B)
4. Connect your Android device (USB debugging enabled) and click **Run**

### Local LLM Model (Optional)

The app supports on-device inference via MediaPipe LlmInference. The model file
(`gemma3-1b-it-int4.litertlm`, ~557 MB) is **not included** in this repository
due to size. To enable local AI:

1. Download the Gemma 3 1B INT4 `.litertlm` model from Google AI Edge
2. Place it in `app/src/main/assets/`
3. Rebuild and run — the app will auto-detect the model

Without the model, the app falls back to a user-configured Gemini API key or
runs in NoOp mode (story features limited, health/exercise/drone features fully
functional).

### Firebase (Optional)

`google-services.json` is excluded from version control. If you want Firebase
Auth or other Firebase features:

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Add an Android app with package name `com.example.isekaikuroshin`
3. Download `google-services.json` and place it in `app/`

### ESP32 Hardware Setup (Optional)

See [`hardware/README.md`](hardware/README.md) for full instructions. Summary:

1. Flash `hardware/firmware/bluetooth_esp32_kolluk_controller.ino` to an ESP32
2. Edit `hardware/elrs_config/user_defines.txt` with a unique binding phrase
3. Wire MPU6050 (SDA=21, SCL=22) and E28 module per the KiCad schematic
4. Pair via BLE from the app's Drone Controller screen

## Screenshots

> Screenshots coming soon. The UI includes a Compose-based dashboard, combat
> interface, inventory grid, health hub with charts, spell studio with particle
> effects, and seal practice with real-time hand tracking.

## Performance

Target device: **Samsung Galaxy A34 5G** (MediaTek Dimensity 1080, 6 GB RAM)

| Metric | Goal |
|--------|------|
| Idle RAM | < 300 MB |
| UI frame rate | 60 FPS stable |
| Thermal | < 45°C during exercise sessions |

See [`PERFORMANCE_GUIDE.md`](PERFORMANCE_GUIDE.md) for detailed optimization notes.

## License

MIT License — see [LICENSE](LICENSE).

## Contributing

Contributions are welcome. Please open an issue first to discuss what you'd like
to change. See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.
