package dev.sakura.server.packet.implemention.s2c;

import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.annotations.ProtocolField;

import java.util.Map;

public class ClientParamsS2C implements IRCPacket {
    @ProtocolField("params")
    private Map<String, String> params;

    public ClientParamsS2C() {
    }

    public ClientParamsS2C(Map<String, String> params) {
        this.params = params;
    }

    public Map<String, String> getParams() {
        return params;
    }
}
