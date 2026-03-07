package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.management.AssetManager;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.GetAssetInfoC2S;
import dev.sakura.server.packet.implemention.s2c.AssetInfoS2C;

import java.io.File;

public class GetAssetInfoHandler implements PacketHandler<GetAssetInfoC2S> {
    @Override
    public void handle(GetAssetInfoC2S packet, Connection connection, UserManager userManager, User user) {
        File file = AssetManager.getAssetFile(packet.getName(), packet.getVersion());
        if (file.exists()) {
            String hash = AssetManager.getFileHash(file);
            connection.sendPacket(new AssetInfoS2C(true, hash, file.length()));
        } else {
            connection.sendPacket(new AssetInfoS2C(false, "", 0));
        }
    }

    @Override
    public boolean allowNull() {
        return true;
    }
}
