package com.ayushi.database.model;

public class Entry {

    private final String value;
    private final long expiryTime;

    public Entry(String value) {
        this.value = value;
        this.expiryTime = -1;
    }

    public Entry(String value, long ttlSeconds) {
        this.value = value;
        this.expiryTime =
                System.currentTimeMillis() + (ttlSeconds * 1000);
    }

    private Entry(String value, long expiryTime, boolean directExpiryTime) {
        this.value = value;
        this.expiryTime = expiryTime;
    }

    public static Entry fromExpiryTime(
            String value,
            long expiryTime
    ) {
        return new Entry(value, expiryTime, true);
    }

    public String getValue() {
        return value;
    }

    public long getExpiryTime() {
        return expiryTime;
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