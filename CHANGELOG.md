# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Boilerplate repository infrastructure (CI/CD, community health templates, security guidelines).
- Comprehensive `.gitignore` to prevent secret leaks.

## [0.0.10] - 2026-06-15

### Added
- Integrated OkHttp WebSockets for direct connection to Twitch IRC.
- Implemented secure token storage using Android's `EncryptedSharedPreferences`.
- Added support for custom Twitch Client ID configuration in settings.
- Initial Jetpack Compose interface with Material 3 styling.
- Native Android Text-to-Speech integration for reading chat messages.
