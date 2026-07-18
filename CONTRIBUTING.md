# Contributing to IsekaiKuroshin

Thank you for your interest in contributing! This project is an independent,
solo-developed open source effort, and all contributions are welcome.

## Getting Started

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes
4. Test on a real device if possible (emulator works for most UI changes)
5. Submit a pull request

## Code Style

- **Kotlin**: Follow the official Kotlin coding conventions (`kotlin.code.style=official`)
- **Compose**: Use `MaterialTheme.colorScheme` — no hardcoded colors or text
- **Localization**: Use `rememberLocalizedText()` for all user-facing strings
- **DI**: Use Hilt for dependency injection — no manual service locators
- **Architecture**: MVVM with ViewModels + StateFlow; Room for persistence

## Pull Request Guidelines

- Reference any related issues in your PR description
- Keep PRs focused — one feature or fix per PR
- Include before/after screenshots for UI changes
- Don't include `google-services.json`, API keys, or model files

## Reporting Issues

When reporting bugs, please include:
- Device model and Android version
- Whether using local LLM or Gemini API
- Steps to reproduce
- Logcat output (if available)

## Areas That Need Help

- UI/UX polish and Material 3 theme improvements
- Additional exercise recognizers (MediaPipe Pose)
- More RAG source texts (public domain novels)
- Localization to languages beyond EN/TR
- ESP32 firmware improvements and additional sensor support
