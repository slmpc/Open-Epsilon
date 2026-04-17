package com.github.epsilon.assets.config;

import com.github.epsilon.Epsilon;
import com.github.epsilon.modules.Module;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

/**
 * Handles one-time migration from the legacy monolithic {@code config.json}
 * to the per-module / per-addon file layout introduced in config version 2.
 *
 * <p>Old layout: {@code epsilon-config/config.json} (all modules in one file)</p>
 * <p>New layout: {@code epsilon-config/{addonId}/{moduleName}.json}</p>
 */
public class LegacyConfigMigrator {

    private final Path configDir;
    private final Gson gson;

    public LegacyConfigMigrator(Path configDir, Gson gson) {
        this.configDir = configDir;
        this.gson = gson;
    }

    /**
     * Returns the per-module config file path for the new layout:
     * {@code {configDir}/{addonId}/{moduleName}.json}
     */
    public Path getModuleFile(Module module) {
        String addonId = module.getAddonId() != null ? module.getAddonId() : "unknown";
        return configDir.resolve(addonId).resolve(module.getName() + ".json");
    }

    /**
     * Checks whether a migration from the old {@code config.json} is needed and,
     * if so, performs it automatically.
     *
     * <p>Migration is triggered when:</p>
     * <ul>
     *   <li>The legacy {@code config.json} file exists, AND</li>
     *   <li>None of the per-module files have been written yet.</li>
     * </ul>
     *
     * <p>After migration, the old file is renamed to {@code config.json.bak}
     * so the migration never runs again.</p>
     *
     * @param modules the full module list (obtained from ModuleManager)
     */
    public void migrateIfNeeded(List<Module> modules) {
        Path legacyConfigFile = configDir.resolve("config.json");
        if (!Files.exists(legacyConfigFile)) return;
        if (modules == null) return;

        // Only migrate when no per-module files have been created yet
        boolean anyExists = modules.stream()
                .filter(Objects::nonNull)
                .anyMatch(m -> Files.exists(getModuleFile(m)));
        if (anyExists) return;

        Epsilon.LOGGER.info("检测到旧版 config.json，正在迁移到按模块分离的配置文件...");
        try {
            String json = Files.readString(legacyConfigFile, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed == null || !parsed.isJsonObject()) return;

            JsonObject root = parsed.getAsJsonObject();
            JsonObject modulesObj = getObject(root, "modules");
            if (modulesObj == null) return;

            for (Module module : modules) {
                if (module == null) continue;
                JsonObject moduleObj = getObject(modulesObj, module.getName());
                if (moduleObj == null) continue;

                Path dest = getModuleFile(module);
                Files.createDirectories(dest.getParent());
                Files.writeString(dest, gson.toJson(moduleObj), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
            }

            // Rename the old file so we never migrate again
            Path backupFile = getAvailableBackupPath();
            Files.move(legacyConfigFile, backupFile);
            Epsilon.LOGGER.info("迁移完成，旧配置已备份为 {}", backupFile.getFileName());
        } catch (Exception e) {
            Epsilon.LOGGER.error("迁移旧版配置失败: {}", legacyConfigFile, e);
        }
    }

    private Path getAvailableBackupPath() {
        Path backupFile = configDir.resolve("config.json.bak");
        if (!Files.exists(backupFile)) {
            return backupFile;
        }

        int index = 1;
        Path candidate;
        do {
            candidate = configDir.resolve("config.json.bak." + index);
            index++;
        } while (Files.exists(candidate));

        return candidate;
    }

    private static JsonObject getObject(JsonObject parent, String key) {
        JsonElement el = parent.get(key);
        return (el != null && el.isJsonObject()) ? el.getAsJsonObject() : null;
    }
}

