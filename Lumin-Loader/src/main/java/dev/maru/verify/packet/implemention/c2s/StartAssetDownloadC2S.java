package dev.maru.verify.packet.implemention.c2s;

import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.annotations.ProtocolField;

public class StartAssetDownloadC2S implements IRCPacket {
    @ProtocolField("name")
    private String name;
    
    @ProtocolField("version")
    private String version;
    
    @ProtocolField("offset")
    private long offset;

    public StartAssetDownloadC2S() {
    }

    public StartAssetDownloadC2S(String name, String version, long offset) {
        this.name = name;
        this.version = version;
        this.offset = offset;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public long getOffset() {
        return offset;
    }
}
