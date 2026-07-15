package com.ayushi.database.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseLogger {

    private static final String LOG_FILE_PATH = "logs/server.log";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DatabaseLogger() {
    }

    public static synchronized void log(
            String clientAddress,
            String command,
            String response
    ) {
        File logFile = new File(LOG_FILE_PATH);

        File parentDirectory = logFile.getParentFile();

        if (parentDirectory != null && !parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }

        String logMessage =
                LocalDateTime.now().format(FORMATTER)
                        + " | "
                        + clientAddress
                        + " | "
                        + command
                        + " | "
                        + response;

        try (
                BufferedWriter writer =
                        new BufferedWriter(
                                new FileWriter(logFile, true)
                        )
        ) {
            writer.write(logMessage);
            writer.newLine();

        } catch (IOException exception) {
            System.out.println(
                    "Unable to write log: "
                            + exception.getMessage()
            );
        }
    }
}