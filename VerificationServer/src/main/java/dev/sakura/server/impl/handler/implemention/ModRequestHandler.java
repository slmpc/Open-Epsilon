package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.management.ModManager;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.RequestModC2S;
import dev.sakura.server.packet.implemention.s2c.ClientParamsS2C;
import dev.sakura.server.packet.implemention.s2c.DownloadModS2C;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModRequestHandler implements PacketHandler<RequestModC2S> {

    private static final Map<String, CachedMod> cache = new ConcurrentHashMap<>();

    @Override
    public void handle(RequestModC2S packet, Connection connection, UserManager userManager, User user) {
        Logger.info("Received mod request from HWID: {} for {}/{}", packet.getHwid(), packet.getName(), packet.getVersion());

        if (!user.isVerifiedIntegrity()) {
            //Logger.warn("User {} requested mod without passing integrity check!", user.getUsername());
            // return;
        }

        try {
            File modFile = ModManager.getModFile(packet.getName(), packet.getVersion());

            if (modFile != null && modFile.exists()) {
                CachedMod cached = getCachedMod(modFile);

                // Send critical runtime parameters
                Map<String, String> params = new HashMap<>();
                params.put("server_token", UUID.randomUUID().toString());
                params.put("timestamp", String.valueOf(System.currentTimeMillis()));

                // You can customize params based on the requested mod
                if (packet.getName() != null) {
                    params.put("mod_id", packet.getName());
                }

                connection.sendPacket(new ClientParamsS2C(params));

                connection.sendPacket(new DownloadModS2C(cached.content, cached.hash));
                Logger.info("Sent mod to HWID: {}", packet.getHwid());
            } else {
                Logger.error("Mod file not found for: " + packet.getName() + " " + packet.getVersion());
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to process mod request");
        }
    }

    private CachedMod getCachedMod(File file) throws IOException {
        String key = file.getAbsolutePath();
        CachedMod cached = cache.get(key);

        if (cached == null || file.lastModified() > cached.lastModified) {
            Logger.info("Reloading mod file from disk: " + file.getName());
            byte[] bytes = Files.readAllBytes(file.toPath());
            String content = Base64.getEncoder().encodeToString(bytes);
            String hash = getMD5(bytes);
            cached = new CachedMod(content, hash, file.lastModified());
            cache.put(key, cached);
        }
        return cached;
    }

    private String getMD5(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public boolean allowNull() {
        return true;
    }

    private record CachedMod(String content, String hash, long lastModified) {
    }

}
