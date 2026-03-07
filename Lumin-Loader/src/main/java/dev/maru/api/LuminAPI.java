package dev.maru.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LuminAPI {

    private static final Map<String, String> runtimeParams = new ConcurrentHashMap<>();

    public static String get(String key) {
        return runtimeParams.get(key);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            String val = get(key);
            return val != null ? Integer.parseInt(val) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static void updateParams(Map<String, String> newParams) {
        if (newParams != null) {
            runtimeParams.putAll(newParams);
        }
    }

    public static boolean isConnected() {
        return runtimeParams.containsKey("server_token");
    }

}
