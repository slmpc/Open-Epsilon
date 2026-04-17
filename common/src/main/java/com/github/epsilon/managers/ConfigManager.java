package com.github.epsilon.managers;

import com.github.epsilon.Epsilon;
import com.github.epsilon.assets.config.LegacyConfigMigrator;
import com.github.epsilon.modules.HudModule;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.settings.impl.*;
import com.google.gson.*;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class ConfigManager {

    private static final int CONFIG_VERSION = 2;

    /**
     * Root directory: epsilon-config/
     */
    private static final Path configDir = Paths.get("epsilon-config");

    private static final Path friendFile = configDir.resolve("friends.json");

    // INSTANCE must be declared AFTER all static fields it depends on
    public static final ConfigManager INSTANCE = new ConfigManager();

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();


    private final LegacyConfigMigrator legacyMigrator = new LegacyConfigMigrator(configDir, gson);

    private ConfigManager() {
    }

    public void initConfig() {
        try {
            Files.createDirectories(configDir);
            legacyMigrator.migrateIfNeeded(ModuleManager.INSTANCE.getModules());
            applyToModules(ModuleManager.INSTANCE.getModules());
            loadFriends();
        } catch (Exception e) {
            Epsilon.LOGGER.error("初始化配置失败", e);
        }
    }

    /**
     * Returns the root config directory (replaces the old single-file getter).
     */
    public synchronized Path getConfigDir() {
        return configDir;
    }

    public synchronized void reload() {
        applyToModules(ModuleManager.INSTANCE.getModules());
        loadFriends();
    }

    public synchronized void applyToModules(List<Module> modules) {
        if (modules == null) return;
        for (Module module : modules) {
            if (module != null) applyModuleFromDisk(module);
        }
    }

    public synchronized void saveNow() {
        List<Module> modules = ModuleManager.INSTANCE.getModules();
        if (modules != null) {
            for (Module module : modules) {
                if (module != null) saveModuleToDisk(module);
            }
        }
        saveFriends();
    }

    // -------------------------------------------------------------------------
    // Per-module file helpers
    // -------------------------------------------------------------------------

    private Path getModuleFile(Module module) {
        return legacyMigrator.getModuleFile(module);
    }

    private void applyModuleFromDisk(Module module) {
        Path file = getModuleFile(module);
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed == null || !parsed.isJsonObject()) return;
            applyModuleObject(module, parsed.getAsJsonObject());
        } catch (Exception e) {
            Epsilon.LOGGER.error("读取模块配置失败: {}", file, e);
        }
    }

    private void applyModuleObject(Module module, JsonObject moduleObj) {
        if (moduleObj.has("keyBind") && moduleObj.get("keyBind").isJsonPrimitive()) {
            try {
                module.setKeyBind(moduleObj.get("keyBind").getAsInt());
            } catch (Exception ignored) {
            }
        }

        if (moduleObj.has("bindMode") && moduleObj.get("bindMode").isJsonPrimitive()) {
            try {
                module.setBindMode(Module.BindMode.valueOf(moduleObj.get("bindMode").getAsString()));
            } catch (Exception ignored) {
            }
        }

        if (moduleObj.has("hidden") && moduleObj.get("hidden").isJsonPrimitive()) {
            try {
                module.setHidden(moduleObj.get("hidden").getAsBoolean());
            } catch (Exception ignored) {
            }
        }

        if (module instanceof HudModule hud) {
            HudModule.HorizontalAnchor horizontalAnchor = readHorizontalAnchor(moduleObj, "hudHorizontalAnchor");
            HudModule.VerticalAnchor verticalAnchor = readVerticalAnchor(moduleObj, "hudVerticalAnchor");
            Float anchorX = readFloat(moduleObj, "hudAnchorX");
            Float anchorY = readFloat(moduleObj, "hudAnchorY");

            if (horizontalAnchor != null && verticalAnchor != null && anchorX != null && anchorY != null) {
                hud.setAnchorState(horizontalAnchor, verticalAnchor, anchorX, anchorY);
            } else {
                Float renderX = readFloat(moduleObj, "hudX");
                Float renderY = readFloat(moduleObj, "hudY");
                if (renderX != null && renderY != null) {
                    hud.loadLegacyPosition(renderX, renderY);
                }
            }
        }

        JsonObject settingsObj = getObject(moduleObj, "settings");
        if (settingsObj != null) {
            for (Setting<?> setting : module.getSettings()) {
                applySetting(setting, settingsObj.get(setting.getName()));
            }
        }

        // Apply enabled state last so onEnable/onDisable fire after settings are set
        if (moduleObj.has("enabled") && moduleObj.get("enabled").isJsonPrimitive()) {
            module.setEnabled(moduleObj.get("enabled").getAsBoolean());
        }
    }

    private void saveModuleToDisk(Module module) {
        Path file = getModuleFile(module);
        try {
            Files.createDirectories(file.getParent());
            String json = gson.toJson(buildModuleObject(module));
            Files.writeString(file, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            Epsilon.LOGGER.error("写入模块配置失败: {}", file, e);
        }
    }

    private JsonObject buildModuleObject(Module module) {
        JsonObject obj = new JsonObject();
        obj.addProperty("version", CONFIG_VERSION);
        obj.addProperty("enabled", module.isEnabled());
        obj.addProperty("keyBind", module.getKeyBind());
        obj.addProperty("bindMode", module.getBindMode().name());
        obj.addProperty("hidden", module.isHidden());

        if (module instanceof HudModule hud) {
            hud.updateLayout(null);
            obj.addProperty("hudX", hud.x);
            obj.addProperty("hudY", hud.y);
            obj.addProperty("hudAnchorX", hud.getAnchorX());
            obj.addProperty("hudAnchorY", hud.getAnchorY());
            obj.addProperty("hudHorizontalAnchor", hud.getHorizontalAnchor().name());
            obj.addProperty("hudVerticalAnchor", hud.getVerticalAnchor().name());
        }

        JsonObject settingsObj = new JsonObject();
        for (Setting<?> setting : module.getSettings()) {
            if (setting == null) continue;
            JsonElement value = serializeSetting(setting);
            if (value != null) settingsObj.add(setting.getName(), value);
        }
        obj.add("settings", settingsObj);

        return obj;
    }


    // -------------------------------------------------------------------------
    // Friends
    // -------------------------------------------------------------------------

    private synchronized void saveFriends() {
        JsonArray array = new JsonArray();
        for (String name : FriendManager.INSTANCE.getFriends()) {
            array.add(name);
        }
        try {
            Files.createDirectories(friendFile.getParent());
            Files.writeString(friendFile, gson.toJson(array), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            Epsilon.LOGGER.error("写入好友文件失败: {}", friendFile, e);
        }
    }

    private synchronized void loadFriends() {
        FriendManager.INSTANCE.clearFriends();
        if (!Files.exists(friendFile)) return;
        try {
            String json = Files.readString(friendFile, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed != null && parsed.isJsonArray()) {
                for (JsonElement el : parsed.getAsJsonArray()) {
                    if (el.isJsonPrimitive()) {
                        FriendManager.INSTANCE.addFriend(el.getAsString());
                    }
                }
            }
        } catch (Exception e) {
            Epsilon.LOGGER.error("读取好友文件失败: {}", friendFile, e);
        }
    }

    // -------------------------------------------------------------------------
    // Setting serialization / deserialization
    // -------------------------------------------------------------------------

    private static JsonElement serializeSetting(Setting<?> setting) {
        if (setting instanceof KeybindSetting s) return new JsonPrimitive(s.getValue());
        if (setting instanceof BoolSetting s) return new JsonPrimitive(s.getValue());
        if (setting instanceof IntSetting s) return new JsonPrimitive(s.getValue());
        if (setting instanceof DoubleSetting s) return new JsonPrimitive(s.getValue());
        if (setting instanceof StringSetting s) return new JsonPrimitive(s.getValue());
        if (setting instanceof EnumSetting s) return new JsonPrimitive(s.getValue().toString());
        if (setting instanceof ColorSetting s) {
            Color c = s.getValue();
            return c == null ? null : new JsonPrimitive(c.getRGB());
        }
        return null;
    }

    private static void applySetting(Setting<?> setting, JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) return;
        try {
            if (setting instanceof BoolSetting s) s.setValue(value.getAsBoolean());
            else if (setting instanceof KeybindSetting s) s.setValue(value.getAsInt());
            else if (setting instanceof IntSetting s) s.setValue(value.getAsInt());
            else if (setting instanceof DoubleSetting s) s.setValue(value.getAsDouble());
            else if (setting instanceof StringSetting s) s.setValue(value.getAsString());
            else if (setting instanceof EnumSetting s) s.setMode(value.getAsString());
            else if (setting instanceof ColorSetting s) {
                int argb = value.getAsInt();
                Color c = new Color(argb, true);
                if (!s.isAllowAlpha()) c = new Color(c.getRed(), c.getGreen(), c.getBlue());
                s.setValue(c);
            }
        } catch (Exception ignored) {
        }
    }

    private static JsonObject getObject(JsonObject parent, String key) {
        JsonElement el = parent.get(key);
        return (el != null && el.isJsonObject()) ? el.getAsJsonObject() : null;
    }

    private static Float readFloat(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) return null;
        try {
            return value.getAsFloat();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static HudModule.HorizontalAnchor readHorizontalAnchor(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) return null;
        try {
            return HudModule.HorizontalAnchor.valueOf(value.getAsString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static HudModule.VerticalAnchor readVerticalAnchor(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) return null;
        try {
            return HudModule.VerticalAnchor.valueOf(value.getAsString());
        } catch (Exception ignored) {
            return null;
        }
    }
}
