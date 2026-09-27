# Nova Agent — Autonomous Android AI Assistant

<p align="center">
  <img src="app/src/main/res/drawable/ic_island_sparkle.xml" width="96" height="96" alt="Nova Agent Logo" />
</p>

**Nova Agent** is an autonomous Android AI Agent application powered by the brain of **Google Gemini (AI Studio API)**, with multi-provider fallback for **OpenRouter** and **OpenAI**. 

Nova Agent can understand voice and text commands, inspect the active Android screen in real-time (**Watchdog**), navigate and interact with third-party and modded apps (scrolling Reels & Shorts on Instagram, YouTube, and Facebook; auto-typing search and chat queries), verify and dial contacts, send SMS messages with user confirmation, intercept unanswered phone calls with an iOS-style **20-second Live Voicemail Assistant**, and display status actions through an interactive **Floating Dynamic Island**.

Created by **[@edit.og_](https://instagram.com/edit.og_)**.

---

## Key Features & Architecture

### 1. Main Chat & UI
* **ChatGPT/Gemini Style Conversational UI**: Rich message bubbles with markdown-like styling, dynamic action badges, and loading states.
* **Creator Identity Link**: Top-left header displays the Agent name ("Nova Agent by @edit.og_") which directly links to the creator's Instagram profile (`@edit.og_`).
* **Delete Full Chat**: Top-right header features a dedicated chat wipe button with a trash can icon and safety confirmation dialog.
* **Hardware & Voice Trigger**: Voice command mic button with live Speech-to-Text and Text-to-Speech (TTS) audio feedback.

### 2. Multi-Provider API Setup (4-Field Architecture)
* **Field 1 (Profile Name)**: Custom configuration label.
* **Field 2 (API Key)**: Secure API key input with show/hide password visibility toggle.
* **Field 3 (Model Selection)**: Support for latest free and high-performance models:
  * Gemini: `gemini-1.5-flash`, `gemini-2.0-flash`, `gemini-1.5-pro`, `gemini-1.0-pro`
  * OpenRouter: `meta-llama/llama-3.1-8b-instruct:free`, `google/gemini-flash-1.5`, `claude-3.5-haiku`
  * OpenAI: `gpt-4o-mini`, `gpt-4o`, `gpt-3.5-turbo`
* **Field 4 (Base URL)**: Customizable endpoint for standard endpoints or local proxy servers.
* **Live Connection Test**: Real-time ping test button to verify key validity before saving.

### 3. iOS-Style 20s Live Voicemail
* **Automated Call Attendant**: Listens for incoming phone calls via `TelephonyManager`. If the call rings unanswered for **20 seconds**, Nova Agent automatically triggers the voicemail attendant service.
* **Automated Voice Greeting**: Speaks a custom greeting to the caller: *"The recipient is currently unavailable. Please leave a voicemail for Nova Agent after the tone."*
* **Inbox & Audio Player**: Lists all received voicemails with caller ID, timestamp, and duration.
* **Listen On/Off Toggle**: Dedicated playback button that starts and stops audio listening with dynamic audio waveforms.
* **Delete Voicemail**: Instant deletion with confirmation.
* **Simulation Mode**: Built-in test trigger button to verify the 20-second unanswered workflow without needing a second phone.

### 4. Advanced Settings & Personalization
* **Theme Color Engine (8 Neon Colors)**:
  `Neon Red`, `Neon Blue`, `Neon Green`, `Neon Yellow`, `Neon Orange`, `Neon White`, `Neon Brown`, `Neon Purple`.
* **23 UI Design Morphisms**:
  1. Soft UI
  2. Brutalism / Neobrutalism
  3. Aero Glassmorphism
  4. Liquid Glass / Liquidmorphism
  5. Auroramorphism
  6. Claymorphism
  7. Skeuomorphism
  8. Glassmorphism
  9. Neumorphism
  10. Frutiger Aero
  11. Y2K UI
  12. Cyberpunk UI
  13. Holographic UI
  14. Material Design
  15. Fluent Design
  16. Metallicmorphism
  17. Glassmorphic Neumorphism
  18. Gradientmorphism
  19. 3D Morphism
  20. Pixel UI
  21. Retro-futuristic UI
  22. Paper / Material Morphism
  23. Inflated UI
* **10 Text Color & Animation Styles**:
  * `Solid [White]`
  * `Gradient [2-4 Colored]`
  * `Aurora [Animated Multicolor Gradient]`
  * `Glow [Custom User Color]`
  * `Glass [Neon Blue Minimal Shimmer]`
  * `Metallic [Chrome / Silver / Gold Reflection]`
  * `Holographic [Iridescent Rainbow Reflection]`
  * `Liquid [Fluid Gradient / Reflection]`
  * `3D [Extrusion + Depth Shadows]`
  * `Outline [Transparent Fill + Colored Stroke]`
* **Dynamic Island Controls**:
  * X-Axis offset slider
  * Y-Axis offset slider
  * Width & Height dimension sliders
  * Corner rounding radius slider
  * Animation timeline smoothness slider
  * Interactive live preview pill

---

## Autonomous Agent Capabilities

1. **Universal App Opener**:
   Queries `PackageManager` to launch any installed application or modded APK (YouTube, Instagram, WhatsApp, TikTok, Chrome, etc.) using fuzzy matching and aliases.
2. **Watchdog Screen Reader**:
   Uses `AccessibilityService` to inspect the `AccessibilityNodeInfo` hierarchy of whichever app the user is actively viewing, allowing Gemini to understand context and predict user goals.
3. **App Interaction & Gestures**:
   * **Reels & Shorts**: Dispatches automated vertical swipes to scroll between Instagram Reels, YouTube Shorts, or Facebook videos.
   * **Media Control**: Taps center coordinate to play/pause video and music players.
   * **Text Entry**: Injects text via `ACTION_SET_TEXT` directly into search bars or chat input boxes across YouTube, Play Store, WhatsApp, Messenger, etc.
4. **Smart Calling & Messaging (With Confirmation)**:
   * Queries device contacts via `ContactsContract`.
   * **Multiple Number Handling**: When multiple phone numbers exist for the same name, displays a structured list with Row numbers (Row 1, Row 2...), prompts the user to select, and requires explicit "Yes/No" confirmation before placing calls or sending SMS.
5. **Hardware Key Shortcut**:
   Intercepts hardware **Volume Up** (`KEYCODE_VOLUME_UP`) key events inside `AgentAccessibilityService.onKeyEvent` to instantly toggle voice listening mode on and off without touching the screen.

---

## Project Structure

```
NovaAgent/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── drawable/          # Vector icons (delete, mic, send, play, stop, voicemail, sparkle)
│       │   ├── values/            # strings.xml, colors.xml, themes.xml
│       │   └── xml/               # accessibility_service_config.xml
│       └── java/com/editog/novaagent/
│           ├── MainActivity.kt
│           ├── NovaApplication.kt
│           ├── automation/        # AppLauncher, ContactsHelper, TelephonyHelper, VoiceManager
│           ├── data/
│           │   ├── api/           # GeminiApiService, OpenAiCompatibleService, AgentBrainOrchestrator
│           │   ├── model/         # ChatMessage, AgentAction, ApiConfig, VoicemailItem, AppSettings
│           │   └── repository/    # ChatRepository, ApiConfigRepository, VoicemailRepository, SettingsRepository
│           ├── service/           # AgentAccessibilityService, DynamicIslandService, WatchDogOverlayService, VoicemailService, CallInterceptionReceiver
│           └── ui/
│               ├── components/    # DynamicIslandPreview, StyledText, ConfirmationDialog
│               ├── navigation/    # BottomNavItem, AppNavHost
│               ├── screens/       # ChatScreen, ApiSetupScreen, VoicemailScreen, SettingsScreen
│               └── theme/         # Color.kt, Type.kt, Theme.kt, UiStyleModifiers.kt
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

---

## How to Build and Run

### Prerequisites
* **Android Studio** Hedgehog (2023.1.1) or newer
* **JDK**: Version 17
* **Android SDK**: Min SDK 26 (Android 8.0), Target SDK 34 (Android 14)

### Steps
1. Clone this repository or open the project folder in Android Studio.
2. Allow Gradle sync to resolve all dependencies from `gradle/libs.versions.toml`.
3. Connect an Android device or launch an Android Emulator.
4. Run the project:
   ```bash
   ./gradlew assembleDebug
   ```
5. On the device, grant the following permissions when prompted:
   * **Accessibility Service**: Go to Settings -> Accessibility -> Installed Services -> Enable **Nova Agent**.
   * **Appear on Top (Overlay)**: Enable Display over other apps for the Dynamic Island floating pill.
   * **Contacts, Phone & SMS**: Required for call verification and SMS dispatch.
   * **Microphone**: Required for voice command listening.
6. Open **API Setup**, paste your **Google AI Studio API Key**, and tap **Save Config**.

---

## Creator
Created by **[@edit.og_](https://instagram.com/edit.og_)**.
