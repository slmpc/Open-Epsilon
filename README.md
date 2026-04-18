# Epsilon Rewrite
<h4 align="center">
    <p>
        <b>English</b> |
        <a href="https://github.com/KonekokoHouse/Epsilon-Rewrite/blob/main/README_zh.md">中文</a>
    </p>
</h4>
A modern multi loader Minecraft utility client built on NeoForge & Fabric with advanced rendering system and modular architecture.

## 🚀 Addon System
[Epsilon Addon Template](https://github.com/KonekokoHouse/Epsilon-Addon-Template)

[Addon Development Guide](docs/addon-development.md)

## 🎨 Graphics System

The Lumin rendering system provides custom render pipelines for:
- Rectangles & Round Rectangles
- Shadows & Blur effects
- TTF Font rendering
- Texture rendering
- Custom vertex formats

See [Lumin Graphics README](src/main/java/com/github/epsilon/graphics/README.md) for details.

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
