package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.ChallengeResponseC2S;
import org.tinylog.Logger;

public class ChallengeResponseHandler implements PacketHandler<ChallengeResponseC2S> {
    @Override
    public void handle(ChallengeResponseC2S packet, Connection connection, UserManager userManager, User user) {
        String expected = user.getExpectedChallengeResponse();
        if (expected != null && expected.equals(packet.getAnswer())) {
            user.setVerifiedIntegrity(true);
            Logger.info("Integrity check passed for user: {}", user.getUsername());
        } else {
            Logger.warn("Integrity check failed for user: {} (Expected: {}, Got: {})",
                    user.getUsername(), expected, packet.getAnswer());
        }
    }

    @Override
    public boolean allowNull() {
        return false;
    }
}
