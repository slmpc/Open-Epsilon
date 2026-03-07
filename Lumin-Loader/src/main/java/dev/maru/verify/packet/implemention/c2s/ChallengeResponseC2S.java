package dev.maru.verify.packet.implemention.c2s;

import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.annotations.ProtocolField;

public class ChallengeResponseC2S implements IRCPacket {
    @ProtocolField("answer")
    private String answer;

    public ChallengeResponseC2S() {
    }

    public ChallengeResponseC2S(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }
}
