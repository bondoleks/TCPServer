package org.example;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class TCPClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String serverAddress = "localhost";
        System.out.print("Enter the server port: ");
        int serverPort = scanner.nextInt();

        try (Socket socket = new Socket(serverAddress, serverPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            boolean[] continueSending = {true}; // Масив, який дозволяє зробити змінну "фактично фінальною"

            // Створюємо окремий потік для обробки відповідей від сервера
            Thread serverResponseThread = new Thread(() -> {
                try {
                    while (continueSending[0]) {
                        String response = reader.readLine();
                        if (response == null) {
                            continueSending[0] = false; // Закриваємо цикл, якщо сервер закрив з'єднання
                        } else {
                            System.out.println("Server response: " + response);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            serverResponseThread.start();

            while (continueSending[0]) {
                System.out.println("Enter a message to send to the server (or type 'exit' to quit):");
                String userMessage = userInput.readLine();

                if ("exit".equalsIgnoreCase(userMessage)) {
                    continueSending[0] = false;
                } else {
                    writer.write(userMessage);
                    writer.newLine();
                    writer.flush();
                }
            }

            // Завершення взаємодії з сервером
            serverResponseThread.join();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
