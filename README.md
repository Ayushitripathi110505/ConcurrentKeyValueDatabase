# Concurrent Key-Value Database

A multithreaded in-memory key-value database built in Java that supports concurrent client connections over TCP sockets. The project demonstrates core Computer Engineering concepts including multithreading, synchronization, networking, caching, persistence, transactions, and concurrent programming.

---

## Features

- Multi-client TCP server
- Thread-safe concurrent operations using Read-Write Locks
- CRUD operations (SET, GET, DELETE)
- Key expiration using TTL
- Background expiry scheduler
- LRU cache eviction
- File-based persistence
- Transaction support (BEGIN, COMMIT, ROLLBACK)
- Graceful server shutdown
- Server logging
- JUnit testing

---

## Tech Stack

- Java 22
- Maven
- Java Socket Programming
- ExecutorService Thread Pool
- ReentrantReadWriteLock
- LinkedHashMap (LRU Cache)
- ScheduledExecutorService
- JUnit 5
- IntelliJ IDEA

---

## Project Architecture

```
                     Client 1
                         │
                     Client 2
                         │
                     Client 3
                         │
                  TCP Server Socket
                         │
              ExecutorService Thread Pool
                         │
                ClientHandler Threads
                         │
                ┌───────────────────┐
                │   KeyValueStore   │
                └───────────────────┘
                   │      │      │
             ReadWrite   TTL    LRU
                Lock    Manager Cache
                   │
          Persistence Manager
                   │
            database.txt
```

---

## Folder Structure

```
src
 ├── client
 ├── server
 ├── storage
 ├── model
 ├── lock
 ├── logger
 ├── utils
 └── test
```

---

## Supported Commands

| Command | Description |
|----------|-------------|
| SET key value | Insert or update a key |
| GET key | Retrieve value |
| DELETE key | Delete key |
| SIZE | Number of keys |
| SETEX key ttl value | Insert key with expiration |
| TTL key | Remaining time before expiration |
| SAVE | Persist database to disk |
| BEGIN | Start transaction |
| COMMIT | Commit transaction |
| ROLLBACK | Discard transaction |
| EXIT | Disconnect client |

---

## Example Session

```
SET name Ayushi
OK

GET name
Ayushi

SETEX session 60 abc123
OK

TTL session
57

BEGIN

SET city Pune

GET city
Pune

ROLLBACK

GET city
KEY_NOT_FOUND

BEGIN

SET city Pune

COMMIT

GET city
Pune
```

---

## Concurrency

The project uses:

- ExecutorService for handling multiple clients.
- ReentrantReadWriteLock for thread-safe concurrent reads and writes.
- ScheduledExecutorService for automatic TTL cleanup.
- Atomic transaction commits using a write lock.

---

## Persistence

The database is automatically saved to disk and restored on server startup.

```
data/database.txt
```

---

## Logging

Every client operation is recorded in:

```
logs/server.log
```

Example:

```
2026-07-16 11:45:22 | /127.0.0.1:53421 | SET name Ayushi | OK
```

---

## Testing

JUnit tests verify:

- SET
- GET
- DELETE
- TTL
- Persistence
- Transactions

All tests currently pass.

---

## Computer Science Concepts Demonstrated

- Socket Programming
- Multithreading
- Thread Pools
- Synchronization
- Read-Write Locks
- Caching
- LRU Eviction
- TTL Expiration
- File Persistence
- Transactions
- Concurrent Programming
- Unit Testing

---

## Future Improvements

- Authentication
- Write Ahead Logging (WAL)
- Replication
- Metrics Dashboard
- REST API
- Docker Deployment

---

## Author

Ayushi Tripathi
