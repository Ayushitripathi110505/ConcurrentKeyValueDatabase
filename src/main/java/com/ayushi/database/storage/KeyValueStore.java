package com.ayushi.database.storage;

import com.ayushi.database.lock.LockManager;
import com.ayushi.database.model.Entry;
import com.ayushi.database.utils.Constants;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

public class KeyValueStore {

    private final LinkedHashMap<String, Entry> store;
    private final Lock readLock;
    private final Lock writeLock;
    private final PersistenceManager persistenceManager;

    public KeyValueStore() {
        LockManager lockManager = new LockManager();

        this.readLock = lockManager.getReadLock();
        this.writeLock = lockManager.getWriteLock();

        this.persistenceManager = new PersistenceManager();
        this.store = persistenceManager.load();

        evictIfRequired();
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
         * LinkedHashMap uses access order.
         * Therefore, get() modifies the internal ordering and needs
         * the write lock.
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

    /*
     * Used by ClientHandler while reading inside a transaction.
     * Entry is immutable, so returning the reference is safe.
     */
    public Entry getEntry(String key) {
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

            return entry;

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

    /*
     * Applies all transaction operations while holding one write lock.
     * Other clients cannot observe a partially committed transaction.
     */
    public void applyTransaction(
            Map<String, Entry> pendingWrites,
            Set<String> pendingDeletes
    ) {
        writeLock.lock();

        try {
            for (String key : pendingDeletes) {
                store.remove(key);
            }

            for (Map.Entry<String, Entry> operation
                    : pendingWrites.entrySet()) {

                Entry entry = operation.getValue();

                if (!entry.hasExpired()) {
                    store.put(operation.getKey(), entry);
                }
            }

            evictIfRequired();

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

    public void save() {
        readLock.lock();

        try {
            persistenceManager.save(store);
        } finally {
            readLock.unlock();
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
                        "LRU eviction: "
                                + leastRecentlyUsed.getKey()
                );

                iterator.remove();
            }
        }
    }
}