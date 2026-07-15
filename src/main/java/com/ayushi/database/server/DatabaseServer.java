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

    public DatabaseServer() {
        this.keyValueStore = new KeyValueStore();

        this.threadPool =
                Executors.newFixedThreadPool(
                        Constants.THREAD_POOL_SIZE
                );

        this.expiryManager =
                new ExpiryManager(keyValueStore);
    }

    public void start() {
        expiryManager.start();

        try (
                ServerSocket serverSocket =
                        new ServerSocket(Constants.PORT)
        ) {
            System.out.println(
                    "Database server started on port "
                            + Constants.PORT
            );

            while (true) {
                Socket clientSocket = serverSocket.accept();

                ClientHandler clientHandler =
                        new ClientHandler(
                                clientSocket,
                                keyValueStore
                        );

                threadPool.submit(clientHandler);
            }

        } catch (IOException exception) {
            System.out.println(
                    "Server error: " + exception.getMessage()
            );
        } finally {
            expiryManager.stop();
            threadPool.shutdown();
        }
    }
}