package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -cp classes sg.edu.nus.iss.baccarat.server.ServerApp <port> <number_of_decks>");
            return;
        }

        int port;
        int numberOfDecks;
        
        try {
            port = Integer.parseInt(args[0]);
            numberOfDecks = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid arguments. Port and number of decks must be integers.");
            return;
        }

        // Create a shuffled deck of cards based on the number of decks provided
        List<String> cards = new ArrayList<>();
        for (int i = 0; i < numberOfDecks; i++) {
            for (int value = 1; value <= 13; value++) {
                // 11 - Jack, 12 - Queen , 13 - King
                for (int suit = 1; suit <= 4; suit++) {
                    cards.add(value + "." + suit);
                }
            }
        }

        Collections.shuffle(cards);

        // Save the shuffled cards to a data file named "cards.db"
        try (FileWriter writer = new FileWriter("cards.db")) {
            for (String card : cards) {
                writer.write(card + "\n");
            }
            System.out.println("Shuffled cards saved to cards.db");
        } catch (IOException e) {
            System.out.println("Error writing to cards.db: " + e.getMessage());
        }

        // Reset game history on server restart
        resetGameHistory();

        // Start the server with a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port + " with " + numberOfDecks + " deck(s) of cards.");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.execute(new BaccaratEngine(clientSocket));
            }
        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }

    private static void resetGameHistory() {
        try (FileWriter fileWriter = new FileWriter("game_history.csv", false)) {
            // Opening the file in overwrite mode without writing anything will empty it
            fileWriter.write("");
            System.out.println("Game history has been reset.");
        } catch (IOException e) {
            System.out.println("Error resetting game history: " + e.getMessage());
        }
    }

}
