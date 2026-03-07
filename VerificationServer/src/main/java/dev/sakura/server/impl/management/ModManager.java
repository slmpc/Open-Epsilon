package dev.sakura.server.impl.management;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModManager {
    private static final File MODS_DIR = new File("mods");
    private static final Pattern FILENAME_PATTERN = Pattern.compile("(.+)_(.+)\\.jar");

    static {
        if (!MODS_DIR.exists()) {
            MODS_DIR.mkdirs();
        }
    }

    public static List<ModInfo> listMods() {
        List<ModInfo> list = new ArrayList<>();
        File[] files = MODS_DIR.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return list;

        for (File f : files) {
            Matcher m = FILENAME_PATTERN.matcher(f.getName());
            if (m.matches()) {
                list.add(new ModInfo(m.group(1), m.group(2), f));
            } else {
                // Fallback for legacy or manually placed files
                // Treat entire name (minus .jar) as name, version as "unknown"
                String rawName = f.getName().substring(0, f.getName().length() - 4);
                if (rawName.equals("default")) {
                    list.add(new ModInfo("Default", "Latest", f));
                } else {
                    list.add(new ModInfo(rawName, "unknown", f));
                }
            }
        }
        return list;
    }

    public static void addMod(File source, String name, String version) throws IOException {
        String filename = name + "_" + version + ".jar";
        // Sanitize filename
        filename = filename.replaceAll("[\\\\/:*?\"<>|]", "");
        File target = new File(MODS_DIR, filename);
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void deleteMod(String name, String version) {
        for (ModInfo mod : listMods()) {
            if (mod.name.equals(name) && mod.version.equals(version)) {
                mod.file.delete();
                return;
            }
        }
    }

    public static File getModFile(String name, String version) {
        for (ModInfo mod : listMods()) {
            if (mod.name.equals(name) && mod.version.equals(version)) {
                return mod.file;
            }
        }
        // Fallback for legacy requests (null name/version)
        if (name == null || version == null) {
            return new File(MODS_DIR, "default.jar");
        }
        return null;
    }

    public static class ModInfo {
        public final String name;
        public final String version;
        public final File file;

        public ModInfo(String name, String version, File file) {
            this.name = name;
            this.version = version;
            this.file = file;
        }

        @Override
        public String toString() {
            return name + " (" + version + ")";
        }
    }
}
