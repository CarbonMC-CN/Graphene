package net.carbonmc.graphene.util;

import java.util.concurrent.ConcurrentHashMap;

public final class LogThrottler {
    private static final long DEFAULT_COOL_DOWN_MS = 60_000L;
    private static final ConcurrentHashMap<String, Long> LAST_PRINT = new ConcurrentHashMap<>();
    public static boolean shouldLog(String key) {
        long now = System.currentTimeMillis();
        Long last = LAST_PRINT.get(key);
        if (last != null && now - last < DEFAULT_COOL_DOWN_MS) {
            return false;
        }
        LAST_PRINT.put(key, now);
        return true;
    }

    private LogThrottler() {}
}