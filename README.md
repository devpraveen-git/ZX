# CallScribe (Call-Assistant)

CallScribe is a modern, native Android application built with **Kotlin, Jetpack Compose, Material 3, MVVM, Hilt, Room, and Coroutines/Flow**. It automatically records phone calls in the background, generates timestamped subtitles (SRT/VTT), and translates transcripts.

---

## Architecture & Technology Stack

* **Language**: Kotlin 2.3.20 (JVM Toolchain 17)
* **UI**: Jetpack Compose + Material 3 Design
* **Architecture**: Clean Architecture + MVVM (Model-View-ViewModel)
* **Dependency Injection**: Dagger Hilt
* **Database & Persistence**: Room with FTS4 (Full-Text Search)
* **Asynchronous Flow**: Kotlin Coroutines & Reactive StateFlow
* **Media & Audio**: MediaRecorder (AAC/m4a @ 16 kHz), Media3 / ExoPlayer
* **Speech-to-Text**: On-device Whisper / Vosk / Sherpa-ONNX architecture + optional Cloud API fallback
* **Translation**: On-device Google ML Kit Translate + Cloud fallback
* **Security & Storage**: AndroidX Security Crypto (`EncryptedSharedPreferences`), BiometricPrompt

---

## Core Features

### 1. AFK & Background Auto-Recording
* **Foreground Service**: `CallRecordingService` operates with persistent notification (`foregroundServiceType="microphone"`).
* **Telephony State Machine**: `PhoneCallStateTracker` accurately handles incoming, outgoing, answered, and disconnected call lifecycles.
* **Recording Rules**:
  * **All Calls**: Automatically records every call.
  * **Unknown Numbers Only**: Skips saved contacts, records unrecognized numbers.
  * **Selected Contacts**: Only records numbers matching user-selected whitelist.
  * **Manual Only**: On-demand recording.
* **Battery & Autostart Optimization**: Exemption helpers for battery optimization and deep links for OEM autostart managers (Xiaomi/HyperOS, Samsung/One UI, Oppo/ColorOS, Vivo/Funtouch, Huawei).

### 2. Audio Capture Strategy (Handling Modern Android Restrictions)
* **Primary Route**: MediaRecorder with `AudioSource.VOICE_RECOGNITION` (or fallback `MIC`) and automatic speakerphone activation (`AudioManager.setSpeakerphoneOn` / `setCommunicationDevice` on API 31+) to record both parties without root or accessibility exploits.
* **Dialer Import Path**: SAF (Storage Access Framework) folder scanner to monitor and ingest native dialer recordings from OEM storage.
* **Audio Format**: AAC in MPEG-4 container (`.m4a`), 16 kHz mono optimized for speech models.
* **Legal Compliance**: Optional automatic beep tone (`AudioAlertHelper`) played at call start to notify parties in dual-consent jurisdictions.

### 3. Subtitle Generation (Speech-to-Text)
* Word/chunk timestamped segments.
* Dual-speaker separation heuristic (`[You]` / `[Other]`).
* Export to standard `.srt`, WebVTT (`.vtt`), and `.txt`.

### 4. Translation
* Offline language translation using Google ML Kit downloadable models.
* Multi-language target selection with dual original/translated subtitle synchronization.

---

## Project Structure

```
CallScribe/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   └── java/com/example/callscribe/
│   │   │       ├── CallScribeApp.kt
│   │   │       ├── MainActivity.kt
│   │   │       ├── data/
│   │   │       │   ├── model/ (Models.kt)
│   │   │       │   ├── local/ (Entities, DAOs, Room Database)
│   │   │       │   └── repository/ (RecordingRepository, SettingsRepository)
│   │   │       ├── recording/ (AudioRecorderEngine, CallRecordingService, CallStateReceiver, etc.)
│   │   │       ├── subtitle/ (SrtGenerator)
│   │   │       ├── di/ (DatabaseModule, RepositoryModule)
│   │   │       └── theme/ (Color, Theme, Type)
│   │   └── test/java/com/example/callscribe/
│   │       ├── recording/ (PhoneCallStateTrackerTest, RecordingRuleEvaluatorTest)
│   │       └── subtitle/ (SrtGeneratorTest)
├── gradle/
│   └── libs.versions.toml (Gradle Version Catalog)
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Known Limitations per Android Version & Device Brand

| Android Version / Brand | Limitation | CallScribe Solution |
| :--- | :--- | :--- |
| **Android 9+ (Pie through 14+)** | `AudioSource.VOICE_CALL` is blocked for third-party non-system apps. | Uses `AudioSource.VOICE_RECOGNITION` with **Speaker Mode Auto-On** to clearly capture both uplink and downlink audio via microphone. |
| **Android 14+ (API 34+)** | Strict foreground service types require explicit `microphone` type and active runtime permission before start. | `CallRecordingService` specifies `foregroundServiceType="microphone"` in manifest and service starts only after permission is verified. |
| **Xiaomi / MIUI / HyperOS** | Aggressive background task killing and restricted autostart. | Guided deep-link to Security App ➔ Autostart Management + Battery Saver set to "No restrictions". |
| **Samsung (One UI)** | Places apps into "Sleeping" or "Deep sleeping" states when inactive. | Guidance to add CallScribe to "Never sleeping apps" in Device Care settings. |
| **Oppo / Realme (ColorOS)** | Kills background services without "Allow Auto-launch" toggle. | Automated intent routing to Startup App List. |
| **Vivo (Funtouch / OriginOS)** | Restricts background microphone and autostart in iManager. | Intent routing to iManager Autostart Whitelist. |

---

## Build & Run Instructions

### Prerequisites
* Java JDK 17
* Android SDK 34+ (Build-Tools & Platform Tools)
* Gradle 8.11+ / Gradle Wrapper

### Run Unit Tests
```bash
./gradlew test
```

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Install onto Device / Emulator
```bash
./gradlew installDebug
```
