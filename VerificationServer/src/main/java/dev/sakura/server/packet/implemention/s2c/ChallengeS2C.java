package dev.sakura.server.packet.implemention.s2c;

import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.annotations.ProtocolField;

public class ChallengeS2C implements IRCPacket {
    @ProtocolField("salt")
    private String salt;

    public ChallengeS2C() {
    }

    public ChallengeS2C(String salt) {
        this.salt = salt;
    }

    public String getSalt() {
        return salt;
    }
}
