package com.sysm.devsync.infrastructure;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class Utils {

    private Utils() {
    }

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

    public static String like(String input) {
        if (input == null) {
            return "";
        }
        return "%" + sanitize(input.toLowerCase()) + "%";
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    }

    public static Instant iTruncatedNow() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public static LocalDateTime ldtTruncatedNow() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
