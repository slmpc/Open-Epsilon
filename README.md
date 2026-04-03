# Lumin Client (Epsilon Rewrite)

A modern Minecraft utility client built on NeoForge 26.1 with advanced rendering system and modular architecture.

## 🚀 Features

- **Modular Design** - 37+ functional modules across Combat, Player, Render, and World categories
- **Custom Rendering Engine** - Lumin rendering system with custom pipelines, shaders, and MD3-themed UI
- **Event-Driven Architecture** - Dynamic event registration with NeoForge event bus
- **Mixin Integration** - 21+ mixin injections for deep game modifications
- **Configuration System** - Flexible settings with multiple data types and dependency support

## 📦 Tech Stack

- **Java 25**
- **NeoForge 26.1.0.7-beta**
- **Minecraft 26.1**
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
