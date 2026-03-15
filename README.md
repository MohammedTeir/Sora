<p align="center">
  <img src=".github/readme-images/app-icon.png" alt="Sora Logo" width="120"/>
</p>

<h1 align="center">Sora</h1>

<p align="center">
  A free and open-source manga reader for Android.
</p>

<p align="center">
  <a href="https://github.com/mahmoud-teir/Sora/actions/workflows/build.yml">
    <img src="https://github.com/mahmoud-teir/Sora/actions/workflows/build.yml/badge.svg" alt="Build Status"/>
  </a>
  <a href="https://github.com/mahmoud-teir/Sora/releases">
    <img src="https://img.shields.io/github/v/release/mahmoud-teir/Sora?style=flat-square" alt="Latest Release"/>
  </a>
  <a href="https://github.com/mahmoud-teir/Sora/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/mahmoud-teir/Sora?style=flat-square" alt="License"/>
  </a>
</p>

---

## 📖 About

**Sora** is a modern, feature-rich manga reader built for Android. Based on the Mihon/Tachiyomi project, Sora brings a refined reading experience with additional features including:

- 🌐 **Arabic & RTL Support** — Full right-to-left layout support
- 🔄 **Built-in Translation** — Powered by ML Kit for real-time text recognition and translation
- 📂 **Hidden Categories** — Organize your library with the ability to hide categories
- 🎨 **Custom Themes** — 20+ built-in themes with custom color picker support
- 📚 **Multi-Source Support** — Browse and read from a wide variety of sources
- 📥 **Local Reading** — Read downloaded manga offline
- 🔍 **Global Search** — Search across all your sources at once
- 📊 **Tracking** — Sync your reading progress with tracking services

## 🛠️ Tech Stack

| Component       | Technology                        |
|-----------------|-----------------------------------|
| Language        | Kotlin                            |
| UI Framework    | Jetpack Compose + Material 3      |
| Architecture    | Multi-module, Clean Architecture  |
| Database        | SQLDelight                        |
| Networking      | OkHttp                            |
| Image Loading   | Coil 3                            |
| DI              | Injekt                            |
| Navigation      | Voyager                           |
| Serialization   | Kotlinx Serialization             |
| Build System    | Gradle (Kotlin DSL)               |

## 📋 Requirements

- Android 8.0 (API 26) or higher
- JDK 17 for building

## 🏗️ Building

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable)
- JDK 17

### Build Steps

```bash
# Clone the repository
git clone https://github.com/mahmoud-teir/Sora.git
cd Sora

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### Build Variants

| Variant     | Description                                  |
|-------------|----------------------------------------------|
| `debug`     | Development build with debugging enabled     |
| `release`   | Production build with code shrinking         |
| `foss`      | Fully open-source build without proprietary dependencies |
| `preview`   | Pre-release build for testing                |

## 📦 Download

Get the latest APK from [GitHub Releases](https://github.com/mahmoud-teir/Sora/releases).

## 📄 License

```
Copyright 2015 Javier Tomás

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 🙏 Acknowledgments

Sora is built upon the incredible work of:

- [Mihon](https://github.com/mihonapp/mihon)
- [Tachiyomi](https://github.com/tachiyomiorg/tachiyomi)
