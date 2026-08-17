# TARZO — Technology-Assisted Responsive & Intelligent Zero-Operator

A production-ready AI voice assistant for Android with Hinglish support, device automation, vision capabilities, and a futuristic interface.

---

## Features

### Voice Assistant
- **Wake Word**: "Bolo TARZO" activation (energy-based VAD + keyword matching)
- **Natural Conversation**: Hindi, English, and Hinglish support
- **TTS Responses**: High-quality Text-to-Speech output
- **Background Listening**: Optional continuous wake-word detection

### Device Control
- Flashlight ON/OFF (via CameraManager)
- Brightness adjustment (requires WRITE_SETTINGS permission)
- Volume control (media, ringtone, alarm streams)
- Wi-Fi status and toggle (opens settings on Android 10+)
- Bluetooth toggle (BLUETOOTH_CONNECT on Android 12+)
- Battery, date, time, and device info
- App launcher via voice commands

### Camera
- Photo capture (rear and front/selfie)
- Video recording with start/stop
- Timer-based capture (configurable countdown)
- Voice confirmation after every action

### Vision & Scene Analysis
- Image analysis via ML Kit (object detection, image labeling)
- OCR text extraction from camera/gallery images
- Scene context explanation
- Gallery image analysis
- Screen text extraction

### Screen Automation
- UI element detection via AccessibilityService
- Button tap automation
- Screen text extraction
- Scroll up/down navigation
- **Safety**: Never performs sensitive actions without explicit confirmation

### Calls & SMS
- Dial contacts by name (voice)
- Send SMS with confirmation prompt
- Incoming caller name announcement
- Contact favorites support

### WhatsApp Integration
- Open WhatsApp via intent
- Prepare messages using Android intents
- Voice/video call flows where supported
- Confirmation before sending consequential messages

### Media Controls
- Spotify/YouTube Music integration via intents
- Play/pause/skip/previous track
- YouTube voice search and playback controls
- Fullscreen and playback speed where supported

### Smart Features
- Google/web search via ACTION_WEB_SEARCH
- Weather information
- News briefings
- Translation (20+ languages, on-device fallback)
- Alarms, timers, and reminders (via AlarmManager)
- Smart scrolling (voice-controlled)

### Memory
- Persistent user-approved preferences
- "Remember this" / "What do you remember?" / "Forget that" commands
- Memory management screen with inspect/delete
- Categories: Preference, Fact, Contact, Setting
- **Security**: Never stores passwords or credentials

### Anti-Theft
- Movement detection (accelerometer)
- Charger-disconnect alert
- Intruder photo (front camera)
- Siren alarm
- Explicit setup and permissions required

### UI/UX
- Premium dark futuristic theme
- Animated TARZO orb (idle/listening/thinking/speaking states)
- Voice waveform visualization
- Quick-action controls
- Conversation history
- Permission center
- Memory manager
- Settings with full configuration
- Privacy dashboard

---

## Architecture

```
┌─────────────────────────────────────────────┐
│                  UI Layer                    │
│  (Jetpack Compose Screens & Components)     │
├─────────────────────────────────────────────┤
│              ViewModel Layer                 │
│  (Hilt ViewModels with StateFlow)           │
├─────────────────────────────────────────────┤
│            Feature Modules                   │
│  Device │ Camera │ Vision │ Communication   │
│  Media  │ Search │ Reminders │ Translation  │
│  Security │ Automation                       │
├─────────────────────────────────────────────┤
│              Core Engine                     │
│  Voice │ AI/Intent │ Permissions │ Storage   │
│  Network │ Utils                          │
├─────────────────────────────────────────────┤
│          Android Framework Layer             │
│  Services │ Receivers │ Providers           │
└─────────────────────────────────────────────┘
```

### Modular Components

| Layer | Components |
|-------|-----------|
| Voice Input | WakeWordEngine, SpeechRecognitionManager, TTSManager |
| AI Engine | IntentDetector (30+ intents), LLMClient (offline + API) |
| Permission | PermissionManager (9 permission groups) |
| Storage | SecureStorage (AES-256-GCM), MemoryManager (Room DB) |
| Network | ApiClient (Retrofit + OkHttp) |
| Services | VoiceAssistantService, WakeWordService, CallDetectionService, AntiTheftService |

---

## Prerequisites

### Development Machine
- **Android Studio**: Iguana (2023.2.1) or newer
- **JDK**: 17 (bundled with Android Studio)
- **Android SDK**: API Level 35
- **Gradle**: 8.11.1 (handled by wrapper)
- **Physical device** recommended (many features require real hardware)

### Build Requirements
- Android Build Tools 35.0.0
- NDK (optional, only if using native wake word models)

---

## Building the APK

### Option 1: Android Studio (Recommended)

1. **Clone/Copy the Project**
   ```bash
   # Copy the TARZO directory to your local machine
   cp -r /path/to/TARZO ~/TARZO
   cd ~/TARZO
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - File → Open → Select the TARZO project directory
   - Wait for Gradle sync to complete (may take 2-5 minutes on first run)

3. **Generate Gradle Wrapper** (if missing)
   ```bash
   # In Android Studio terminal:
   gradle wrapper --gradle-version 8.11.1
   ```

4. **Build Debug APK**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Or use: `./gradlew assembleDebug`
   - Output: `app/build/outputs/apk/debug/app-debug.apk`

5. **Build Release APK**
   - Build → Generate Signed Bundle / APK
   - Select APK
   - Create or select a keystore
   - Select release build variant
   - Or use: `./gradlew assembleRelease`
   - Output: `app/build/outputs/apk/release/app-release.apk`

6. **Build App Bundle (AAB) for Play Store**
   ```bash
   ./gradlew bundleRelease
   ```
   - Output: `app/build/outputs/bundle/release/app-release.aab`

### Option 2: Command Line

```bash
# Navigate to project directory
cd TARZO

# Ensure gradlew is executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config in build.gradle)
./gradlew assembleRelease

# Build AAB
./gradlew bundleRelease

# Clean build
./gradlew clean assembleDebug
```

### Option 3: Without Android Studio (CI/CD)

```bash
# Install SDK command-line tools
# Set ANDROID_HOME environment variable
export ANDROID_HOME=/path/to/android/sdk

# Accept licenses
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses

# Build
./gradlew assembleDebug --no-daemon
```

---

## Installing on Device

### Via ADB

```bash
# Enable USB debugging on device
# Connect device via USB

# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Install release APK
adb install app/build/outputs/apk/release/app-release.apk

# Launch app
adb shell am start -n com.tarzo.ai/.MainActivity
```

### Via File Transfer
1. Copy the APK to your phone
2. Open the APK file on your phone
3. Enable "Install from unknown sources" if prompted
4. Tap Install

---

## First-Time Setup

1. **Grant Permissions**: The app will request permissions as needed. Go to Settings → Permissions for TARZO to manage.

2. **Enable Wake Word**: Go to Settings → Wake Word → Enable. This starts the background listening service.

3. **Configure Language**: Go to Settings → Language. Select Hindi, English, or Hinglish.

4. **Set Up AI Backend** (optional): For enhanced AI responses, configure your backend API URL in Settings → AI Model. Without a backend, TARZO uses its built-in offline response engine.

5. **Enable Accessibility** (for screen automation): Go to Settings → Accessibility → TARZO → Enable.

6. **Set Up Anti-Theft** (optional): Go to Security → Anti-Theft. Enable desired features.

---

## Backend Integration (Optional)

TARZO works fully offline for intent detection and basic responses. For advanced AI capabilities:

### Setting Up the Backend Server

1. Create a backend server (Node.js, Python FastAPI, etc.)
2. Implement these endpoints:
   - `POST /api/chat` — AI chat completions
   - `POST /api/translate` — Translation
   - `GET /api/weather` — Weather data
   - `GET /api/news` — News briefing
3. Configure the base URL in TARZO Settings → AI Model
4. **Never put API keys in the APK** — keep them on the server

### Example Backend (Python FastAPI)

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

class ChatRequest(BaseModel):
    query: str
    intent: str
    params: dict = {}

@app.post("/api/chat")
async def chat(req: ChatRequest):
    # Integrate with OpenAI/Gemini/etc. here
    return {"response": "Your AI response"}

@app.post("/api/translate")
async def translate(text: str, source: str, target: str):
    return {"translated": "Translated text"}
```

---

## Project Structure

```
TARZO/
├── app/
│   ├── src/main/
│   │   ├── java/com/tarzo/ai/
│   │   │   ├── MainActivity.kt              # Main entry point
│   │   │   ├── TarzoApp.kt                  # Application class
│   │   │   ├── core/                        # Core engine
│   │   │   │   ├── voice/                   # WakeWord, STT, TTS
│   │   │   │   ├── ai/                      # Intent detection, LLM
│   │   │   │   ├── permissions/             # Permission manager
│   │   │   │   ├── storage/                 # Secure storage, Room DB
│   │   │   │   └── network/                 # API client
│   │   │   ├── features/                    # Feature modules
│   │   │   │   ├── device/                  # Device control
│   │   │   │   ├── camera/                  # Camera operations
│   │   │   │   ├── vision/                  # Vision/OCR (ML Kit)
│   │   │   │   ├── communication/           # Calls, SMS
│   │   │   │   ├── media/                   # YouTube, Spotify, music
│   │   │   │   ├── search/                  # Web search, weather
│   │   │   │   ├── reminders/               # Alarms, timers, reminders
│   │   │   │   ├── translation/             # Translation
│   │   │   │   ├── security/                # Anti-theft
│   │   │   │   └── automation/              # Screen automation
│   │   │   ├── ui/                          # UI Layer
│   │   │   │   ├── theme/                   # Colors, typography, theme
│   │   │   │   ├── components/              # Reusable components
│   │   │   │   ├── screens/                 # 7 main screens
│   │   │   │   └── navigation/              # Nav graph, routes
│   │   │   ├── services/                    # Foreground services
│   │   │   ├── receiver/                    # Broadcast receivers
│   │   │   └── util/                        # Constants, Result
│   │   ├── res/                             # Android resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml                   # Dependency versions
│   └── wrapper/
├── build.gradle.kts                         # Root build file
├── settings.gradle.kts                      # Project settings
└── gradle.properties
```

---

## Android API Limitations

TARZO only uses official Android APIs. Here are the known limitations:

| Feature | Limitation | TARZO's Approach |
|---------|-----------|-----------------|
| Brightness | WRITE_SETTINGS requires special permission | Opens system settings on API 23+ |
| Wi-Fi Toggle | Restricted on Android 10+ | Opens Wi-Fi settings panel |
| Bluetooth Toggle | Requires BLUETOOTH_CONNECT on 12+ | Checks permission, opens settings if missing |
| Call Control | ANSWER_PHONE_CALLS limited | Provides notification actions where possible |
| Background Recording | Foreground service required | Uses proper foreground service with notification |
| WhatsApp Messages | No official API | Uses intents, confirms before sending |
| Screen Automation | Requires Accessibility Service | Guides user to enable, only acts on explicit command |
| SMS | SEND_SMS permission required | Always confirms before sending |
| Intruder Photo | Front camera access in background | Uses ACTION_IMAGE_CAPTURE, may not work on all devices |

---

## Security

- **No API keys in APK**: All sensitive credentials stay on the backend server
- **Encrypted Storage**: Uses AndroidX Security Crypto (AES-256-GCM)
- **Room Database**: For non-sensitive persistent memory only
- **Runtime Permissions**: All permissions requested at runtime with explanations
- **No Secret Monitoring**: Anti-theft features require explicit user setup
- **HTTPS Enforced**: Network security config blocks cleartext except localhost
- **ProGuard**: Enabled for release builds to obfuscate code

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.1.0 |
| Min SDK | 28 (Android 9.0) |
| Target SDK | 35 (Android 15) |
| UI | Jetpack Compose + Material3 |
| DI | Hilt 2.53.1 |
| Navigation | Navigation Compose 2.8.5 |
| Storage | Room 2.6.1 + EncryptedSharedPreferences |
| Camera | CameraX 1.4.1 |
| Vision | ML Kit (Text, Objects, Labels) |
| Media | Media3 1.5.1 |
| Network | Retrofit 2.11.0 + OkHttp 4.12.0 |
| Build | Gradle 8.11.1 + KSP |

---

## Voice Commands Reference

### Device Control
- "TARZO, flashlight on / off"
- "TARZO, brightness up / down"
- "TARZO, volume up / down"
- "TARZO, Wi-Fi on / off"
- "TARZO, Bluetooth on / off"
- "TARZO, battery kya hai"
- "TARZO, time kya hai / date batao"

### Apps & Media
- "TARZO, YouTube kholo"
- "TARZO, YouTube kholo aur JEE Physics search karo"
- "TARZO, WhatsApp kholo"
- "TARZO, Spotify kholo"
- "TARZO, gaana bajao / music play karo"
- "TARZO, gaana band karo / pause"
- "TARZO, next song / skip"

### Communication
- "TARZO, Mummy ko call karo"
- "TARZO, 9876543210 pe call karo"
- "TARZO, Papa ko SMS bhejo"
- "TARZO, Ravi ko bolo ki main late aaunga"

### Camera & Vision
- "TARZO, photo lo"
- "TARZO, selfie lo"
- "TARZO, video record karo"
- "TARZO, 5 second timer se photo lo"
- "TARZO, image analyze karo"
- "TARZO, text read karo"

### Reminders
- "TARZO, 7 baje alarm laga do"
- "TARZO, 10 minute ka timer lagao"
- "TARZO, kal 3 baje reminder rakh do meeting ka"
- "TARZO, alarm list dikhao"

### Memory
- "TARZO, yaad rakhna mujhe coffee pasand hai"
- "TARZO, kya yaad hai tumhare paas"
- "TARZO, wo bhool do"

### Search & Info
- "TARZO, weather batao"
- "TARZO, news sunao"
- "TARZO, Google pe search karo best phones 2025"
- "TARZO, translate karo hello to Hindi"

### Security
- "TARZO, anti-theft on karo"
- "TARZO, security mode activate karo"

---

## Troubleshooting

### Build Issues
- **Gradle sync fails**: Ensure you have JDK 17 and Android SDK 35 installed
- **Missing dependencies**: Run `./gradlew clean build --refresh-dependencies`
- **Compose compilation error**: Ensure Kotlin plugin version matches Compose BOM

### Runtime Issues
- **Wake word not detected**: Speak clearly and loudly; the energy-based VAD works best in quiet environments
- **TTS not speaking**: Check if TTS engine is installed (Settings → Language & Input → Text-to-Speech)
- **Camera not working**: Grant Camera permission; some devices restrict background camera access
- **Accessibility not working**: Go to Settings → Accessibility → TARZO → Enable
- **Flashlight not working**: Ensure no other app is using the camera

---

## License

This project is provided as-is for educational and personal use. Ensure compliance with all applicable laws when using anti-theft, call recording, and monitoring features.

---

**TARZO — Your AI-Powered Voice Assistant for Android**