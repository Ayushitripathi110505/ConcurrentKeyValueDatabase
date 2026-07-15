package com.ayushi.database.client;

import com.ayushi.database.utils.Constants;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class DatabaseClient {

    public static void main(String[] args) {

        try (
                Socket socket = new Socket("localhost", Constants.PORT);

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));

                PrintWriter writer =
                        new PrintWriter(socket.getOutputStream(), true);

                Scanner scanner = new Scanner(System.in);
        ) {

            System.out.println(reader.readLine());
            System.out.println(reader.readLine());

            while (true) {

                System.out.print("> ");

                String command = scanner.nextLine();

                writer.println(command);

                System.out.println(reader.readLine());

                if (command.equalsIgnoreCase("EXIT"))
                    break;
            }

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

}