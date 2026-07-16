package com.ayushi.database.server;

import com.ayushi.database.logger.DatabaseLogger;
import com.ayushi.database.model.Entry;
import com.ayushi.database.storage.KeyValueStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final KeyValueStore keyValueStore;

    private boolean transactionActive;

    /*
     * New or updated values that have not yet been committed.
     */
    private final Map<String, Entry> pendingWrites;

    /*
     * Keys marked for deletion during the transaction.
     */
    private final Set<String> pendingDeletes;

    public ClientHandler(
            Socket clientSocket,
            KeyValueStore keyValueStore
    ) {
        this.clientSocket = clientSocket;
        this.keyValueStore = keyValueStore;

        this.transactionActive = false;
        this.pendingWrites = new LinkedHashMap<>();
        this.pendingDeletes = new HashSet<>();
    }

    @Override
    public void run() {
        System.out.println(
                "Client connected: "
                        + clientSocket.getRemoteSocketAddress()
        );

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        clientSocket.getInputStream()
                                )
                        );

                PrintWriter writer =
                        new PrintWriter(
                                clientSocket.getOutputStream(),
                                true
                        )
        ) {
            writer.println(
                    "Connected to Concurrent Key-Value Database"
            );

            writer.println(
                    "Commands: SET, SETEX, GET, DELETE, SIZE, TTL, "
                            + "SAVE, BEGIN, COMMIT, ROLLBACK, EXIT"
            );

            String request;

            while ((request = reader.readLine()) != null) {
                String response = processCommand(request);

                writer.println(response);

                DatabaseLogger.log(
                        clientSocket
                                .getRemoteSocketAddress()
                                .toString(),
                        request,
                        response
                );

                if (request.trim().equalsIgnoreCase("EXIT")) {
                    break;
                }
            }

        } catch (IOException exception) {
            System.out.println(
                    "Client connection error: "
                            + exception.getMessage()
            );

        } finally {
            /*
             * Any uncommitted transaction is automatically discarded
             * when the client disconnects.
             */
            clearTransaction();

            try {
                clientSocket.close();
            } catch (IOException exception) {
                System.out.println(
                        "Unable to close client socket: "
                                + exception.getMessage()
                );
            }

            System.out.println("Client disconnected");
        }
    }

    private String processCommand(String request) {
        if (request == null || request.isBlank()) {
            return "ERROR: Command cannot be empty";
        }

        String[] parts = request.trim().split("\\s+", 4);
        String command = parts[0].toUpperCase();

        switch (command) {

            case "BEGIN":
                return beginTransaction();

            case "COMMIT":
                return commitTransaction();

            case "ROLLBACK":
                return rollbackTransaction();

            case "SET":
                return processSet(parts);

            case "SETEX":
                return processSetEx(parts);

            case "GET":
                return processGet(parts);

            case "DELETE":
                return processDelete(parts);

            case "SIZE":
                if (transactionActive) {
                    return "ERROR: SIZE is unavailable inside a transaction";
                }

                return String.valueOf(keyValueStore.size());

            case "TTL":
                if (parts.length < 2) {
                    return "ERROR: Use TTL key";
                }

                return processTTL(parts[1]);

            case "SAVE":
                if (transactionActive) {
                    return "ERROR: Commit or rollback before SAVE";
                }

                keyValueStore.save();
                return "DATABASE_SAVED";

            case "EXIT":
                if (transactionActive) {
                    clearTransaction();

                    return "Connection closed; "
                            + "uncommitted transaction rolled back";
                }

                return "Connection closed";

            default:
                return "ERROR: Invalid command";
        }
    }

    private String beginTransaction() {
        if (transactionActive) {
            return "ERROR: Transaction already active";
        }

        transactionActive = true;
        pendingWrites.clear();
        pendingDeletes.clear();

        return "TRANSACTION_STARTED";
    }

    private String commitTransaction() {
        if (!transactionActive) {
            return "ERROR: No active transaction";
        }

        keyValueStore.applyTransaction(
                pendingWrites,
                pendingDeletes
        );

        clearTransaction();

        return "TRANSACTION_COMMITTED";
    }

    private String rollbackTransaction() {
        if (!transactionActive) {
            return "ERROR: No active transaction";
        }

        clearTransaction();

        return "TRANSACTION_ROLLED_BACK";
    }

    private String processSet(String[] parts) {
        if (parts.length < 3) {
            return "ERROR: Use SET key value";
        }

        String key = parts[1];
        String value = parts[2];

        /*
         * Because split has a limit of 4, a value containing spaces
         * may occupy parts[2] and parts[3].
         */
        if (parts.length == 4) {
            value = parts[2] + " " + parts[3];
        }

        if (transactionActive) {
            pendingWrites.put(key, new Entry(value));
            pendingDeletes.remove(key);

            return "QUEUED";
        }

        keyValueStore.set(key, value);

        return "OK";
    }

    private String processSetEx(String[] parts) {
        if (parts.length < 4) {
            return "ERROR: Use SETEX key seconds value";
        }

        String key = parts[1];

        try {
            long seconds = Long.parseLong(parts[2]);

            if (seconds <= 0) {
                return "ERROR: TTL must be greater than 0";
            }

            String value = parts[3];

            if (transactionActive) {
                pendingWrites.put(
                        key,
                        new Entry(value, seconds)
                );

                pendingDeletes.remove(key);

                return "QUEUED";
            }

            keyValueStore.setWithTTL(
                    key,
                    value,
                    seconds
            );

            return "OK";

        } catch (NumberFormatException exception) {
            return "ERROR: TTL must be a number";
        }
    }

    private String processGet(String[] parts) {
        if (parts.length < 2) {
            return "ERROR: Use GET key";
        }

        String key = parts[1];

        if (transactionActive) {
            /*
             * Read-your-own-writes behavior.
             */
            if (pendingDeletes.contains(key)) {
                return "KEY_NOT_FOUND";
            }

            Entry pendingEntry = pendingWrites.get(key);

            if (pendingEntry != null) {
                if (pendingEntry.hasExpired()) {
                    pendingWrites.remove(key);
                    return "KEY_NOT_FOUND";
                }

                return pendingEntry.getValue();
            }
        }

        String value = keyValueStore.get(key);

        return value == null
                ? "KEY_NOT_FOUND"
                : value;
    }

    private String processDelete(String[] parts) {
        if (parts.length < 2) {
            return "ERROR: Use DELETE key";
        }

        String key = parts[1];

        if (transactionActive) {
            boolean exists =
                    pendingWrites.containsKey(key)
                            || (
                            !pendingDeletes.contains(key)
                                    && keyValueStore.containsKey(key)
                    );

            if (!exists) {
                return "KEY_NOT_FOUND";
            }

            pendingWrites.remove(key);
            pendingDeletes.add(key);

            return "QUEUED";
        }

        boolean deleted = keyValueStore.delete(key);

        return deleted
                ? "DELETED"
                : "KEY_NOT_FOUND";
    }

    private String processTTL(String key) {
        if (transactionActive) {
            if (pendingDeletes.contains(key)) {
                return "-2";
            }

            Entry pendingEntry = pendingWrites.get(key);

            if (pendingEntry != null) {
                if (pendingEntry.hasExpired()) {
                    pendingWrites.remove(key);
                    return "-2";
                }

                return String.valueOf(
                        pendingEntry.getRemainingSeconds()
                );
            }
        }

        return String.valueOf(
                keyValueStore.getTTL(key)
        );
    }

    private void clearTransaction() {
        transactionActive = false;
        pendingWrites.clear();
        pendingDeletes.clear();
    }
}