# Addon Development Guide

本文档介绍如何为 Epsilon 开发 Addon，并同时兼容 Fabric 与 NeoForge。

## 1. 基础 API

推荐使用 `common` 中的统一 API：

- `com.github.epsilon.addon.EpsilonAddon`
- `com.github.epsilon.addon.EpsilonAddonSetupEvent`

> `neoforge` 包下的旧类型仍可用，但已标记 `@Deprecated`，新项目建议全部迁移到 `common` API。

## 2. 编写 Addon 类

最小 Addon 示例：

```java
package your.mod.addon;

import com.github.epsilon.addon.EpsilonAddon;

public class ExampleAddon extends EpsilonAddon {

    public ExampleAddon() {
        super("example_addon");
    }

    @Override
    public void onSetup() {
        // 在这里注册模块
        // registerModule(new YourModule());
    }
}
```

## 3. Fabric 接入（自定义 Entrypoint）

### 3.1 实现 Entrypoint 接口

```java
package your.mod.fabric;

import com.github.epsilon.addon.EpsilonAddonSetupEvent;
import com.github.epsilon.fabric.addon.FabricEpsilonAddonEntrypoint;
import your.mod.addon.ExampleAddon;

public class ExampleFabricAddonEntrypoint implements FabricEpsilonAddonEntrypoint {

    @Override
    public void registerAddon(EpsilonAddonSetupEvent event) {
        event.registerAddon(new ExampleAddon());
    }
}
```

### 3.2 在 `fabric.mod.json` 注册自定义 entrypoint

```json
{
  "entrypoints": {
    "open_epsilon:addon": [
      "your.mod.fabric.ExampleFabricAddonEntrypoint"
    ]
  }
}
```

Epsilon Fabric 会在客户端初始化时自动读取 `open_epsilon:addon` 并注册 addon。

## 4. NeoForge 接入（事件总线）

NeoForge 仍通过 Epsilon 事件总线收集 addon：

```java
package your.mod.neoforge;

import com.github.epsilon.addon.EpsilonAddonSetupEvent;
import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.bus.EventHandler;
import your.mod.addon.ExampleAddon;

public class ExampleNeoHook {

    public ExampleNeoHook() {
        EpsilonEventBus.INSTANCE.subscribe(this);
    }

    @EventHandler
    private void onAddonSetup(EpsilonAddonSetupEvent event) {
        event.registerAddon(new ExampleAddon());
    }
}
```

## 5. 异常隔离行为

Epsilon 已实现两层隔离：

1. **Fabric entrypoint 隔离**：单个 entrypoint 抛异常时，只会记录错误日志，不会阻断其他 addon 的注册。
2. **Addon setup 隔离**：单个 addon 的 `onSetup()` 失败时，只会记录错误日志，不会阻断其他 addon 的加载。

建议在 addon 内部继续做好自身异常处理，避免注册到一半时产生不可预期状态。

## 6. 调试建议

- 检查日志关键词：
  - `Loaded Epsilon addon:`
  - `Failed to register addon entrypoint from mod:`
  - `Failed to setup Epsilon addon:`
- 首次接入时先做一个最小 addon（仅日志输出），确认生命周期后再逐步注册模块。

