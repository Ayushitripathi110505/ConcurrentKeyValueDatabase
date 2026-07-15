package com.ayushi.database.storage;

import com.ayushi.database.lock.LockManager;
import com.ayushi.database.model.Entry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class KeyValueStore {

    private final Map<String, Entry> store;
    private final Lock readLock;
    private final Lock writeLock;

    public KeyValueStore() {
        this.store = new HashMap<>();

        LockManager lockManager = new LockManager();

        this.readLock = lockManager.getReadLock();
        this.writeLock = lockManager.getWriteLock();
    }

    public void set(String key, String value) {
        writeLock.lock();

        try {
            store.put(key, new Entry(value));
        } finally {
            writeLock.unlock();
        }
    }

    public void setWithTTL(
            String key,
            String value,
            long ttlSeconds
    ) {
        writeLock.lock();

        try {
            store.put(key, new Entry(value, ttlSeconds));
        } finally {
            writeLock.unlock();
        }
    }

    public String get(String key) {
        readLock.lock();

        try {
            Entry entry = store.get(key);

            if (entry == null) {
                return null;
            }

            if (!entry.hasExpired()) {
                return entry.getValue();
            }

        } finally {
            readLock.unlock();
        }

        // Remove expired entry using write lock
        deleteExpiredKey(key);

        return null;
    }

    private void deleteExpiredKey(String key) {
        writeLock.lock();

        try {
            Entry currentEntry = store.get(key);

            if (currentEntry != null && currentEntry.hasExpired()) {
                store.remove(key);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public boolean delete(String key) {
        writeLock.lock();

        try {
            return store.remove(key) != null;
        } finally {
            writeLock.unlock();
        }
    }

    public int size() {
        removeExpiredKeys();

        readLock.lock();

        try {
            return store.size();
        } finally {
            readLock.unlock();
        }
    }

    public long getTTL(String key) {
        readLock.lock();

        try {
            Entry entry = store.get(key);

            if (entry == null || entry.hasExpired()) {
                return -2;
            }

            return entry.getRemainingSeconds();

        } finally {
            readLock.unlock();
        }
    }

    public void removeExpiredKeys() {
        writeLock.lock();

        try {
            Iterator<Map.Entry<String, Entry>> iterator =
                    store.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, Entry> mapEntry = iterator.next();

                if (mapEntry.getValue().hasExpired()) {
                    iterator.remove();
                }
            }

        } finally {
            writeLock.unlock();
        }
    }
}