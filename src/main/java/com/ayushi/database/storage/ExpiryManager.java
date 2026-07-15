package com.ayushi.database.storage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExpiryManager {

    private final KeyValueStore keyValueStore;
    private final ScheduledExecutorService scheduler;

    public ExpiryManager(KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
        this.scheduler =
                Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                keyValueStore::removeExpiredKeys,
                5,
                5,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        scheduler.shutdown();
    }
}