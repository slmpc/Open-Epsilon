package dev.maru.verify.packet.implemention.s2c;

import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.annotations.ProtocolField;

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
