package com.sysm.devsync.infrastructure;

public final class Utils {

    private Utils() {}

    public static String generateId() {
        return java.util.UUID.randomUUID().toString();
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
    }
}
