package dev.sakura.server.packet.implemention.s2c;

import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.annotations.ProtocolField;

public class AssetInfoS2C implements IRCPacket {
    @ProtocolField("exists")
    private boolean exists;
    
    @ProtocolField("hash")
    private String hash;
    
    @ProtocolField("size")
    private long size;

    public AssetInfoS2C() {
    }

    public AssetInfoS2C(boolean exists, String hash, long size) {
        this.exists = exists;
        this.hash = hash;
        this.size = size;
    }

    public boolean isExists() {
        return exists;
    }

    public String getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }
}
