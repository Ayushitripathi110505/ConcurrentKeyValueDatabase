package com.ayushi.database.storage;

import com.ayushi.database.model.Entry;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class PersistenceManager {

    private static final String FILE_PATH = "data/database.txt";

    public void save(Map<String, Entry> store) {
        File file = new File(FILE_PATH);

        File parentDirectory = file.getParentFile();

        if (parentDirectory != null && !parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(file)
        )) {
            for (Map.Entry<String, Entry> mapEntry : store.entrySet()) {
                Entry entry = mapEntry.getValue();

                if (!entry.hasExpired()) {
                    writer.write(
                            mapEntry.getKey()
                                    + "\t"
                                    + entry.getValue()
                                    + "\t"
                                    + entry.getExpiryTime()
                    );

                    writer.newLine();
                }
            }

            System.out.println("Database saved successfully");

        } catch (IOException exception) {
            System.out.println(
                    "Unable to save database: "
                            + exception.getMessage()
            );
        }
    }

    public LinkedHashMap<String, Entry> load() {
        LinkedHashMap<String, Entry> loadedStore =
                new LinkedHashMap<>(16, 0.75f, true);

        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return loadedStore;
        }

        try (BufferedReader reader = new BufferedReader(
                new FileReader(file)
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t", 3);

                if (parts.length != 3) {
                    continue;
                }

                String key = parts[0];
                String value = parts[1];
                long expiryTime = Long.parseLong(parts[2]);

                Entry entry = Entry.fromExpiryTime(
                        value,
                        expiryTime
                );

                if (!entry.hasExpired()) {
                    loadedStore.put(key, entry);
                }
            }

            System.out.println("Database loaded successfully");

        } catch (IOException | NumberFormatException exception) {
            System.out.println(
                    "Unable to load database: "
                            + exception.getMessage()
            );
        }

        return loadedStore;
    }
}