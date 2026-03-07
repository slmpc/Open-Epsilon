package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.management.AssetManager;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.StartAssetDownloadC2S;
import dev.sakura.server.packet.implemention.s2c.AssetChunkS2C;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class StartAssetDownloadHandler implements PacketHandler<StartAssetDownloadC2S> {
    private static final int CHUNK_SIZE = 512 * 1024; // 512KB

    @Override
    public void handle(StartAssetDownloadC2S packet, Connection connection, UserManager userManager, User user) {
        File file = AssetManager.getAssetFile(packet.getName(), packet.getVersion());
        if (!file.exists()) {
            return;
        }

        long offset = packet.getOffset();
        if (offset < 0 || offset >= file.length()) {
            offset = 0;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            fis.skip(offset);
            byte[] buffer = new byte[CHUNK_SIZE];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                boolean last = (offset + read) >= file.length();
                byte[] chunk = (read == CHUNK_SIZE) ? buffer : Arrays.copyOf(buffer, read);
                connection.sendPacket(new AssetChunkS2C(chunk, offset, last));
                offset += read;
                
                // Optional: sleep to throttle if needed, but TCP handles it.
            }
            Logger.info("Sent asset file {} to {}", file.getName(), user.getUsername());
        } catch (IOException e) {
            Logger.error(e, "Failed to send asset file");
        }
    }

    @Override
    public boolean allowNull() {
        return true;
    }
}
