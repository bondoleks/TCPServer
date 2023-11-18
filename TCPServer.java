package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class TCPServer {
    private static Map<String, String> commandResponses = new HashMap<>();

    private static final String COMMANDS_FILE = "Commands.txt";

    private static Map<Socket, AtomicInteger> clientCounters = new HashMap<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the port number to listen on: ");
        int port = scanner.nextInt();

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                Thread clientThread = new Thread(() -> handleClient(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            AtomicInteger clientMessageCounter = clientCounters.computeIfAbsent(clientSocket, k -> new AtomicInteger(1));

            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(() -> {
                try {
                    String currentTime = getCurrentTime();

                    String message = "Counter " + clientMessageCounter + ", Time " + currentTime;

                    clientMessageCounter.addAndGet(1);

                    writer.write(message);
                    writer.newLine();
                    writer.flush();
                } catch (SocketException ex) {
                    System.err.println("Client disconnected unexpectedly.");
                    executorService.shutdownNow();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 0, 10, TimeUnit.SECONDS);

            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                loadCommandsFromFile();
                System.out.println("Received from client: " + inputLine);

                String result;
                if (commandResponses.containsKey(inputLine)) {
                    result = commandResponses.get(inputLine);
                } else {
                    result = processInput(inputLine);
                }

                writer.write("Server answer: " + result);
                writer.newLine();
                writer.flush();
                writeToLogFile(inputLine, result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadCommandsFromFile() {
        Map<String, String> newCommandResponses = new HashMap<>();
        try {
            BufferedReader fileReader = new BufferedReader(new FileReader(COMMANDS_FILE));
            String line;

            while ((line = fileReader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String storedCommand = parts[0].trim();
                    String storedResponse = parts[1].trim();
                    newCommandResponses.put(storedCommand, storedResponse);
                }
            }

            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        commandResponses = newCommandResponses;
    }

    private static String processInput(String input) {
        if (input.matches(".*[a-zA-Z].*")) {
            return manipulateString(input);
        } else if (input.matches("\\d+")) {
            int number = Integer.parseInt(input);
            return String.valueOf(number * 1000);
        } else {
            return "Command not found";
        }
    }

    private static String manipulateString(String input) {
        StringBuilder result = new StringBuilder();
        boolean toUpperCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isLetter(c)) {
                if (toUpperCase) {
                    result.append(Character.toUpperCase(c));
                } else {
                    result.append(Character.toLowerCase(c));
                }
                toUpperCase = !toUpperCase;
            } else if (c == ' ') {
                result.append('_');
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private static void writeToLogFile(String inputLine, String result) {
        try (FileWriter fileWriter = new FileWriter("LogFile.txt", true);
             BufferedWriter logWriter = new BufferedWriter(fileWriter)) {
            logWriter.write("Received: " + inputLine);
            logWriter.newLine();
            logWriter.write("Sent: Server answer: " + result);
            logWriter.newLine();
            logWriter.write("------------------------------------------------");
            logWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date now = new Date();
        return dateFormat.format(now);
    }
}