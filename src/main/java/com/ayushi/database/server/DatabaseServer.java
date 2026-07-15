package com.ayushi.database.server;

import com.ayushi.database.storage.ExpiryManager;
import com.ayushi.database.storage.KeyValueStore;
import com.ayushi.database.utils.Constants;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseServer {

    private final KeyValueStore keyValueStore;
    private final ExecutorService threadPool;
    private final ExpiryManager expiryManager;

    private ServerSocket serverSocket;
    private volatile boolean running;

    public DatabaseServer() {
        this.keyValueStore = new KeyValueStore();

        this.threadPool =
                Executors.newFixedThreadPool(
                        Constants.THREAD_POOL_SIZE
                );

        this.expiryManager =
                new ExpiryManager(keyValueStore);

        this.running = false;
    }

    public void start() {
        running = true;
        expiryManager.start();

        try {
            serverSocket = new ServerSocket(Constants.PORT);

            System.out.println(
                    "Database server started on port "
                            + Constants.PORT
            );

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    ClientHandler clientHandler =
                            new ClientHandler(
                                    clientSocket,
                                    keyValueStore
                            );

                    threadPool.submit(clientHandler);

                } catch (IOException exception) {
                    if (running) {
                        System.out.println(
                                "Client connection error: "
                                        + exception.getMessage()
                        );
                    }
                }
            }

        } catch (IOException exception) {
            System.out.println(
                    "Server error: " + exception.getMessage()
            );
        } finally {
            shutdown();
        }
    }

    public synchronized void shutdown() {
        if (!running) {
            return;
        }

        System.out.println("Shutting down database server...");

        running = false;

        keyValueStore.save();
        expiryManager.stop();
        threadPool.shutdown();

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException exception) {
                System.out.println(
                        "Unable to close server socket: "
                                + exception.getMessage()
                );
            }
        }

        System.out.println("Database server stopped safely");
    }
}