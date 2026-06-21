<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" alt="Licha Logo" width="120px" height="120px">
</p>

<h1 align="center">Licha - Twitch TTS 📨📢</h1>

<p align="center">
  <a href="https://github.com/lagosproject/Licha/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/lagosproject/Licha/ci.yml?branch=main&style=flat-square" alt="Build Status">
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/lagosproject/Licha?style=flat-square" alt="License">
  </a>
  <a href="https://kotlinlang.org/">
    <img src="https://img.shields.io/badge/Kotlin-1.9.22-purple?style=flat-square&logo=kotlin" alt="Kotlin Version">
  </a>
  <a href="https://developer.android.com/">
    <img src="https://img.shields.io/badge/Android-API%2024%2B-green?style=flat-square&logo=android" alt="Android SDK">
  </a>
  <a href="https://developer.android.com/jetpack/compose">
    <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue?style=flat-square&logo=jetpackcompose" alt="Compose">
  </a>
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.LakesCorp.TwitchChatTTS">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" width="200">
  </a>
</p>

Licha (TwitchChatTTS) is a beautiful, modern Android application that connects to your Twitch chat and reads messages aloud using Android's native Text-to-Speech (TTS) engine. It is designed to he[...]

---

## 📖 Table of Contents
- [Screenshots](#screenshots)
- [About the Project](#about-the-project)
- [Key Features](#key-features)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [Usage](#usage)
- [Contributing](#contributing)
- [Security](#security)
- [License](#license)

---

## 📸 Screenshots

<p align="center">
  <img src="screenshots_automation/output/phone/en-US/showcase_login.png" alt="Login Screen" width="30%">
  <img src="screenshots_automation/output/phone/en-US/showcase_chat.png" alt="Chat Screen" width="30%">
  <img src="screenshots_automation/output/phone/en-US/showcase_settings.png" alt="Settings Screen" width="30%">
</p>

<p align="center">
  <img src="screenshots_automation/output/tablet_10/en-US/showcase_login.png" alt="Tablet Login" width="30%">
  <img src="screenshots_automation/output/tablet_10/en-US/showcase_chat.png" alt="Tablet Chat" width="30%">
  <img src="screenshots_automation/output/tablet_10/en-US/showcase_settings.png" alt="Tablet Settings" width="30%">
</p>

---

## ℹ️ About the Project

Licha provides a hands-free way to stay connected with your Twitch community. By logging in securely via OAuth, Licha connects to the Twitch IRC server over WebSockets and uses the Android Text-to[...]

---

## ✨ Key Features

- 👂 **Real-time Text-to-Speech**: Listen to Twitch chat messages in real time.
- 🔒 **Secure OAuth Login**: One-tap authentication via Twitch's browser OAuth flow — no manual tokens needed.
- 🛡️ **Credential Security**: OAuth tokens are stored securely using Android's `EncryptedSharedPreferences`.
- 🎨 **Modern Interface**: Designed using Jetpack Compose and Material 3 with support for dark mode.
- 🚀 **WebSocket Connection**: Low-latency chat synchronization using OkHttp WebSockets.

---

## 🛠️ Architecture & Tech Stack

Licha follows modern Android development practices and architecture:

*   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for a fully declarative UI.
*   **Design System**: [Material 3](https://m3.material.io/) for modern styling and colors.
*   **Architecture Pattern**: MVVM (Model-View-ViewModel) with clean separation of concerns.
*   **Dependency Injection**: [Dagger Hilt](https://developer.android.com/training/dependency-injection/hilt-android) for robust, compile-time DI.
*   **Network & WebSockets**: [OkHttp](https://square.github.io/okhttp/) for WebSocket chat connections and API validation.
*   **Local Storage**: `DataStore Preferences` for app settings and `EncryptedSharedPreferences` for sensitive credentials.

---

## 📋 Prerequisites

To build and run Licha, ensure you have:
- **Android Studio Jellyfish / Koala** (or newer)
- **JDK 17** configured in Android Studio
- **Android Device** or Emulator running API level 24 (Android 7.0) or higher
- A **Twitch Developer Account** (to register an app and get a Client ID)

---

## 🚀 Installation & Setup

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/lagosproject/Licha.git
    cd Licha
    ```

2.  **Open in Android Studio**
    *   Launch Android Studio, select **Open**, and navigate to the cloned `Licha` folder.
    *   Allow Gradle to sync and download the required dependencies.

3.  **Run the Application**
    *   Connect your Android device via USB (with Developer Options and USB Debugging enabled) or start an Emulator.
    *   Click the **Run** button (green play icon) in the Android Studio toolbar.

---

## ⚙️ Configuration

Licha supports dynamic configuration of your Twitch API credentials.

### Setting up Twitch Developer Credentials
1.  Go to the [Twitch Developer Console](https://dev.twitch.tv/console).
2.  Register a new application:
    *   **Name**: Choose a unique name (e.g., `MyTwitchChatTTS`).
    *   **OAuth Redirect URLs**: Add `http://localhost`.
    *   **Category**: Select `Application Integration`.
3.  Obtain your **Client ID**.

### Environment Setup
Add your Client ID to `local.properties` (this file is gitignored and never committed):
```properties
TWITCH_CLIENT_ID=your_twitch_client_id_here
```
The build system injects it automatically into `BuildConfig.TWITCH_CLIENT_ID` at compile time.

---

## 💡 Usage

1.  **Authenticate**: Tap **Authorize with Twitch** — a browser window will open to complete Twitch OAuth. The app handles the redirect automatically.
2.  **Connect**: Input your **Channel Name** (e.g., the Twitch channel you want to listen to) and tap **Connect**.
3.  **Listen**: Put on your headphones and hear the chat!

---

## 🤝 Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](file:///CONTRIBUTING.md) for guidelines on how to fork the repository, create feature branches, and submit Pull Requests.

---

## 🛡️ Security

To report a security vulnerability, please refer to our [SECURITY.md](file:///SECURITY.md) guidelines. Never report security issues via public GitHub issues.

---

## 📄 License

This project is licensed under the MIT License. See [LICENSE](file:///LICENSE) for details.
