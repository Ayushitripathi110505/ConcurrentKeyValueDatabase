package com.ayushi.database.storage;

import com.ayushi.database.lock.LockManager;
import com.ayushi.database.model.Entry;
import com.ayushi.database.utils.Constants;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class KeyValueStore {

    private final LinkedHashMap<String, Entry> store;
    private final Lock readLock;
    private final Lock writeLock;

    public KeyValueStore() {
        this.store = new LinkedHashMap<>(
                Constants.MAX_CAPACITY,
                0.75f,
                true
        );

        LockManager lockManager = new LockManager();

        this.readLock = lockManager.getReadLock();
        this.writeLock = lockManager.getWriteLock();
    }

    public void set(String key, String value) {
        writeLock.lock();

        try {
            store.put(key, new Entry(value));
            evictIfRequired();
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
            evictIfRequired();
        } finally {
            writeLock.unlock();
        }
    }

    public String get(String key) {
        /*
         * LinkedHashMap changes its internal access order during get().
         * Therefore, get() must use the write lock instead of the read lock.
         */
        writeLock.lock();

        try {
            Entry entry = store.get(key);

            if (entry == null) {
                return null;
            }

            if (entry.hasExpired()) {
                store.remove(key);
                return null;
            }

            return entry.getValue();

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

    public boolean containsKey(String key) {
        writeLock.lock();

        try {
            Entry entry = store.get(key);

            if (entry == null) {
                return false;
            }

            if (entry.hasExpired()) {
                store.remove(key);
                return false;
            }

            return true;

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
        writeLock.lock();

        try {
            Entry entry = store.get(key);

            if (entry == null) {
                return -2;
            }

            if (entry.hasExpired()) {
                store.remove(key);
                return -2;
            }

            return entry.getRemainingSeconds();

        } finally {
            writeLock.unlock();
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

    private void evictIfRequired() {
        while (store.size() > Constants.MAX_CAPACITY) {
            Iterator<Map.Entry<String, Entry>> iterator =
                    store.entrySet().iterator();

            if (iterator.hasNext()) {
                Map.Entry<String, Entry> leastRecentlyUsed =
                        iterator.next();

                System.out.println(
                        "LRU eviction: " + leastRecentlyUsed.getKey()
                );

                iterator.remove();
            }
        }
    }
}