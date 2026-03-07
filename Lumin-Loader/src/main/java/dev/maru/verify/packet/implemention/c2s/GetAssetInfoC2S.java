package dev.maru.verify.packet.implemention.c2s;

import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.annotations.ProtocolField;

public class GetAssetInfoC2S implements IRCPacket {
    @ProtocolField("name")
    private String name;
    
    @ProtocolField("version")
    private String version;

    public GetAssetInfoC2S() {
    }

    public GetAssetInfoC2S(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
}
