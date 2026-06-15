# Contributing to Licha (TwitchChatTTS)

Thank you for your interest in contributing to Licha! We welcome community contributions to help improve this Twitch text-to-speech app.

Please take a moment to review this guide before submitting your contribution.

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](file:///CODE_OF_CONDUCT.md).

## Getting Started

### 1. Fork and Clone
1. Fork the repository on GitHub.
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Licha.git
   cd Licha
   ```
3. Set the upstream remote to stay up to date:
   ```bash
   git remote add upstream https://github.com/lagosproject/Licha.git
   ```

### 2. Branching Strategy
Create a feature branch from the `main` branch before making your changes. Use descriptive names:
- For features: `feature/your-feature-name`
- For bug fixes: `bugfix/your-bug-name`
- For chore/documentation tasks: `chore/your-task-name`

```bash
git checkout -b feature/cool-new-setting
```

### 3. Development Guidelines
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Keep code clean, modular, and use Dagger Hilt for dependency injection where appropriate.
- Ensure Jetpack Compose components follow Material 3 design guidelines.

### 4. Running Tests & Linting
Before opening a Pull Request, run local tests and linting checks to make sure everything builds and functions correctly:
*   Run unit tests:
    ```bash
    ./gradlew test
    ```
*   Run lint checks:
    ```bash
    ./gradlew lint
    ```

### 5. Prevent Secret Leakage
- **CRITICAL**: Never commit sensitive API tokens, keystores, or password credentials.
- Ensure your `.env` or `local.properties` contains only placeholders and that any new secret fields are added to `.gitignore`.
- Run a local secret check or review files via `git diff --cached` before committing.

## Submitting a Pull Request

1. Push your branch to your GitHub fork:
   ```bash
   git push origin feature/cool-new-setting
   ```
2. Open a Pull Request from your branch to the `lagosproject/Licha` `main` branch.
3. Complete the [Pull Request Template](file:///.github/PULL_REQUEST_TEMPLATE.md) checklists.
4. Maintainers will review your PR and suggest changes or merge it.

Thank you for contributing!
