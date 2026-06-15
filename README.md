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

Licha (TwitchChatTTS) is a beautiful, modern Android application that connects to your Twitch chat and reads messages aloud using Android's native Text-to-Speech (TTS) engine. It is designed to help stream broadcasters and moderators listen to their channel's chat conversations seamlessly without looking at a screen.

---

## 📖 Table of Contents
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

## ℹ️ About the Project

Licha provides a hands-free way to stay connected with your Twitch community. By logging in securely via OAuth, Licha connects to the Twitch IRC server over WebSockets and uses the Android Text-to-Speech API to vocalize incoming chat messages. This makes it perfect for VR streamers, simulator pilots, console players, or anyone who wants to hear chat while focusing on gameplay.

---

## ✨ Key Features

- 👂 **Real-time Text-to-Speech**: Listen to Twitch chat messages in real time.
- 🔒 **Secure OAuth Login**: Authenticate directly with Twitch using secure client credentials.
- 🛡️ **Credential Security**: OAuth tokens are stored securely using Android's `EncryptedSharedPreferences`.
- ⚙️ **Custom Client ID**: Easily configure your own Twitch Application Client ID or use the default.
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
You can reference the [.env.example](file:///.env.example) template to configure build configurations.
If you wish to hardcode a default Client ID for your own custom build, you can set `DEFAULT_CLIENT_ID` in [ChatViewModel.kt](file:///home/vant/Documentos/Licha/app/src/main/java/com/lakescorp/twitchchattts/ChatViewModel.kt#L48):
```kotlin
const val DEFAULT_CLIENT_ID = "your_twitch_client_id_here"
```

---

## 💡 Usage

1.  **Configure Credentials**: Open Licha on your device and input your **Twitch Client ID**.
2.  **Authenticate**: Click **Login** to authorize the application. This will open a browser window requesting access to read and write in your Twitch chat.
3.  **Get Token**: Copy the generated OAuth token and paste it back into Licha.
4.  **Connect**: Input your **Channel Name** (e.g., the Twitch channel you want to listen to) and click **Connect**.
5.  **Listen**: Put on your headphones and hear the chat!

---

## 🤝 Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](file:///CONTRIBUTING.md) for guidelines on how to fork the repository, create feature branches, and submit Pull Requests.

---

## 🛡️ Security

To report a security vulnerability, please refer to our [SECURITY.md](file:///SECURITY.md) guidelines. Never report security issues via public GitHub issues.

---

## 📄 License

This project is licensed under the MIT License. See [LICENSE](file:///LICENSE) for details.
