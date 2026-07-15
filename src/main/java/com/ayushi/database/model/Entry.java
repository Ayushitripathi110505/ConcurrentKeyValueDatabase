package com.ayushi.database.model;

public class Entry {

    private final String value;
    private final long expiryTime;

    // Normal key without expiration
    public Entry(String value) {
        this.value = value;
        this.expiryTime = -1;
    }

    // Key with TTL
    public Entry(String value, long ttlSeconds) {
        this.value = value;
        this.expiryTime =
                System.currentTimeMillis() + (ttlSeconds * 1000);
    }

    public String getValue() {
        return value;
    }

    public boolean hasExpired() {
        return expiryTime != -1
                && System.currentTimeMillis() >= expiryTime;
    }

    public boolean hasExpiry() {
        return expiryTime != -1;
    }

    public long getRemainingSeconds() {
        if (!hasExpiry()) {
            return -1;
        }

        long remaining =
                (expiryTime - System.currentTimeMillis()) / 1000;

        return Math.max(remaining, 0);
    }
}