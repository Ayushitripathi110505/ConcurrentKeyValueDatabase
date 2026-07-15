package com.ayushi.database.lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockManager {

    private final ReentrantReadWriteLock readWriteLock =
            new ReentrantReadWriteLock(true);

    public Lock getReadLock() {
        return readWriteLock.readLock();
    }

    public Lock getWriteLock() {
        return readWriteLock.writeLock();
    }
}