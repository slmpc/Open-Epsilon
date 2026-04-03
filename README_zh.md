# Lumin Client (Epsilon Rewrite)

基于 NeoForge 26.1 构建的现代化 Minecraft 辅助客户端，拥有先进的渲染系统和模块化架构。

## 🚀 核心特性

- **模块化设计** - 37+ 功能模块，涵盖战斗、玩家、渲染、世界四大类别
- **自定义渲染引擎** - Lumin 渲染系统，支持自定义管线、着色器和 MD3 主题 UI
- **事件驱动架构** - 基于 NeoForge 事件总线的动态事件注册机制
- **Mixin 集成** - 21+ Mixin 注入，实现深度的游戏修改
- **配置系统** - 灵活的设置框架，支持多种数据类型和依赖关系

## 📦 技术栈

- **Java 25**
- **NeoForge 26.1.0.7-beta**
- **Minecraft 26.1**
- **Gradle** 构建系统
- **Mixin** 字节码操作框架

## 🏗️ 项目架构

```
src/main/java/com/github/epsilon/
├── managers/      # 核心系统管理器
├── modules/       # 功能模块 (战斗/玩家/渲染/世界)
├── graphics/      # Lumin 渲染引擎
├── gui/          # MD3 主题 UI 系统
├── events/       # 自定义事件系统
├── mixins/       # 游戏注入
├── settings/     # 配置框架
└── utils/        # 工具库
```

## 🎨 渲染系统

Lumin 渲染系统提供自定义渲染管线，支持：
- 矩形与圆角矩形
- 阴影与模糊效果
- TTF 字体渲染
- 纹理渲染
- 自定义顶点格式

详见 [渲染系统文档](src/main/java/com/github/epsilon/graphics/README_zh.md)

## ⚙️ 构建与运行

```bash
# 构建模组
./gradlew build

# 运行客户端
./gradlew runClient
```

## 📂 模块分类

- **Combat (战斗)** - AimAssist, KillAura, CrystalAura, Velocity 等模块
- **Player (玩家)** - AutoSprint, Scaffold, NoSlow, ElytraFly 等模块
- **Render (渲染)** - ESP, Fullbright, NameTags, HUD 等模块
- **World (世界)** - AutoFarm, Stealer, FakePlayer, AutoAccount 等模块

## 📝 许可证

本项目采用多许可证模式分发：

- **项目核心**: 遵循 [Apache License 2.0](LICENSE) 许可证
- **渲染系统**: 核心渲染组件（位于 `src/main/java/com/github/epsilon/graphics/`）
  遵循 [MIT License](src/main/java/com/github/epsilon/graphics/LICENSE) 许可证

---

版权所有 © 2026 KonekokoHouse.
