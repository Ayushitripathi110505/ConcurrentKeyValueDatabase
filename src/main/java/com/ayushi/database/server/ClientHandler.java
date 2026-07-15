package com.ayushi.database.server;

import com.ayushi.database.storage.KeyValueStore;
import com.ayushi.database.logger.DatabaseLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final KeyValueStore keyValueStore;

    public ClientHandler(
            Socket clientSocket,
            KeyValueStore keyValueStore
    ) {
        this.clientSocket = clientSocket;
        this.keyValueStore = keyValueStore;
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
                    "Commands: SET, SETEX, GET, DELETE, SIZE, TTL, SAVE, EXIT"
            );

            String request;

            while ((request = reader.readLine()) != null) {

                String response = processCommand(request);

                writer.println(response);

                DatabaseLogger.log(
                        clientSocket.getRemoteSocketAddress().toString(),
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

        String[] parts = request.trim().split("\\s+");
        String command = parts[0].toUpperCase();

        switch (command) {
            case "SET":
                if (parts.length < 3) {
                    return "ERROR: Use SET key value";
                }

                keyValueStore.set(parts[1], parts[2]);

                return "OK";

            case "SETEX":
                if (parts.length < 4) {
                    return "ERROR: Use SETEX key seconds value";
                }

                try {
                    long seconds = Long.parseLong(parts[2]);

                    if (seconds <= 0) {
                        return "ERROR: TTL must be greater than 0";
                    }

                    keyValueStore.setWithTTL(
                            parts[1],
                            parts[3],
                            seconds
                    );

                    return "OK";

                } catch (NumberFormatException exception) {
                    return "ERROR: TTL must be a number";
                }

            case "GET":
                if (parts.length < 2) {
                    return "ERROR: Use GET key";
                }

                String value = keyValueStore.get(parts[1]);

                return value == null
                        ? "KEY_NOT_FOUND"
                        : value;

            case "DELETE":
                if (parts.length < 2) {
                    return "ERROR: Use DELETE key";
                }

                boolean deleted =
                        keyValueStore.delete(parts[1]);

                return deleted
                        ? "DELETED"
                        : "KEY_NOT_FOUND";

            case "SIZE":
                return String.valueOf(
                        keyValueStore.size()
                );

            case "TTL":
                if (parts.length < 2) {
                    return "ERROR: Use TTL key";
                }

                return String.valueOf(
                        keyValueStore.getTTL(parts[1])
                );

            case "SAVE":
                keyValueStore.save();
                return "DATABASE_SAVED";

            case "EXIT":
                return "Connection closed";

            default:
                return "ERROR: Invalid command";
        }
    }
}