package com.github.epsilon.assets.i18n;

import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class I18NFileGenerator {

    private static final String PREFIX = "epsilon.";

    public static void generate(String filePath) {
        JsonObject root = new JsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        for (Category category : Category.values()) {
            String catKey = PREFIX + "categories." + category.toString().toLowerCase();
            root.addProperty(catKey, "");
        }

        root.addProperty(PREFIX + "keybind.none", "");
        root.addProperty(PREFIX + "keybind.toggle", "");
        root.addProperty(PREFIX + "keybind.hold", "");

        root.addProperty(PREFIX + "gui.search", "");
        root.addProperty(PREFIX + "gui.gameaccount", "");

        for (Module module : ModuleManager.INSTANCE.getModules()) {
            String modulePrefix = "modules." + module.getName().toLowerCase();
            root.addProperty(PREFIX + modulePrefix, "");

            for (Setting<?> setting : module.getSettings()) {
                String settingKey = PREFIX + modulePrefix + "." + setting.getName().toLowerCase();
                root.addProperty(settingKey, "");

                if (setting instanceof EnumSetting<?> enumSetting) {
                    for (final var mode : enumSetting.getModes()) {
                        root.addProperty(settingKey + "." + mode.toString().toLowerCase(), "");
                    }
                }
            }
        }

        final var file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(root, writer);
            System.out.println("I18N file generated successfully at: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
