# Epsilon Rewrite
<h4 align="center">
    <p>
        <b>English</b> |
        <a href="https://github.com/KonekokoHouse/Epsilon-Rewrite/blob/main/README_zh.md">中文</a>
    <p>
</h4>
A modern Minecraft utility client built on NeoForge 26.1 with advanced rendering system and modular architecture.

## 🚀 Features

- **Custom Rendering Engine** - Lumin rendering system with custom pipelines, shaders, and MD3-themed UI
- **Event-Driven Architecture** - Dynamic event registration with NeoForge event bus
- **Configuration System** - Flexible settings with multiple data types and dependency support

## 📦 Tech Stack

- **Java 25**
- **NeoForge 26.1.1.1-beta**
- **Minecraft 26.1.1**
- **Gradle** build system
- **Mixin** for bytecode manipulation

## 🏗️ Architecture

```
src/main/java/com/github/epsilon/
├── managers/      # Core system managers
├── modules/       # Functional modules (Combat/Player/Render/World)
├── graphics/      # Lumin rendering engine
├── gui/          # MD3-themed UI system
├── events/       # Custom event system
├── mixins/       # Game injections
├── settings/     # Configuration framework
└── utils/        # Utility libraries
```

## 🎨 Graphics System

The Lumin rendering system provides custom render pipelines for:
- Rectangles & Round Rectangles
- Shadows & Blur effects
- TTF Font rendering
- Texture rendering
- Custom vertex formats

See [Graphics README](src/main/java/com/github/epsilon/graphics/README.md) for details.

## ⚙️ Building

```bash
# Build the mod
./gradlew build

# Run client
./gradlew runClient
```

## 📝 License

This project is distributed under a multi-license model:

- **Project Core**: Licensed under the [Apache License 2.0](LICENSE).
- **Graphics**: The core rendering components (located in `src/main/java/com/github/epsilon/graphics/`)
  are licensed under the [MIT License](src/main/java/com/github/epsilon/graphics/LICENSE).

---

Copyright © 2026 KonekokoHouse.
