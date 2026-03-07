package dev.sakura.server.impl.user;

public class User {
    private final String sessionId;
    private final String username;
    private final String token;
    private String ign;
    private long expireAt;
    private String prefix;
    private boolean verifiedIntegrity = false;
    private String expectedChallengeResponse;

    public User(String sessionId, String username, String token) {
        this.sessionId = sessionId;
        this.username = username;
        this.token = token;
        this.ign = "Unknown";
        this.expireAt = 0;
        this.prefix = "";
    }

    public User(String sessionId, String username, String token, long expireAt) {
        this.sessionId = sessionId;
        this.username = username;
        this.token = token;
        this.ign = "Unknown";
        this.expireAt = expireAt;
        this.prefix = "";
    }

    public User(String sessionId, String username, String token, long expireAt, String prefix) {
        this.sessionId = sessionId;
        this.username = username;
        this.token = token;
        this.ign = "Unknown";
        this.expireAt = expireAt;
        this.prefix = prefix == null ? "" : prefix;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public String getIgn() {
        return ign;
    }

    public void setIgn(String ign) {
        this.ign = ign;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(long expireAt) {
        this.expireAt = expireAt;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
    }

    public boolean isVerifiedIntegrity() {
        return verifiedIntegrity;
    }

    public void setVerifiedIntegrity(boolean verifiedIntegrity) {
        this.verifiedIntegrity = verifiedIntegrity;
    }

    public String getExpectedChallengeResponse() {
        return expectedChallengeResponse;
    }

    public void setExpectedChallengeResponse(String expectedChallengeResponse) {
        this.expectedChallengeResponse = expectedChallengeResponse;
    }
}

