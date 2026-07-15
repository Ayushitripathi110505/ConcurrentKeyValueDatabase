package com.ayushi.database;

import com.ayushi.database.server.DatabaseServer;

public class Main {

    public static void main(String[] args) {

        DatabaseServer databaseServer =
                new DatabaseServer();

        Runtime.getRuntime().addShutdownHook(
                new Thread(databaseServer::shutdown)
        );

        databaseServer.start();
    }
}