package dev.sakura.server.impl.management;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;

public class AssetManager {
    private static final File ASSETS_DIR = new File("mods/assets");

    static {
        if (!ASSETS_DIR.exists()) {
            ASSETS_DIR.mkdirs();
        }
    }

    public static File getAssetFile(String name, String version) {
        // Sanitize
        String filename = (name + "_" + version + ".zip").replaceAll("[\\\\/:*?\"<>|]", "");
        return new File(ASSETS_DIR, filename);
    }

    public static String getFileHash(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(bytes));
        } catch (Exception e) {
            return "";
        }
    }
}
