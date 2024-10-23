package server;

import java.io.*;
import java.math.BigInteger;
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
}

class BaccaratEngine implements Runnable {
    private Socket clientSocket;
    private List<String> cards;
    List<String> gameHistory = new ArrayList<>();


    public BaccaratEngine(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.cards = loadCards();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String inputLine;
            BigInteger betAmount = BigInteger.ZERO;
            emptyGameHistory();
            while ((inputLine = in.readLine()) != null) {
                String[] commandParts = inputLine.split("\\|");
                String command = commandParts[0];
                switch (command) {
                    case "login":
                        String username = commandParts[1];
                        BigInteger balance = new BigInteger(commandParts[2]);
                        try (FileWriter writer = new FileWriter(username + ".db")) {
                            writer.write(String.valueOf(balance));
                        }
                        out.println("User " + username + " logged in with balance: " + balance);
                        break;
                    case "bet":
                        System.out.println(commandParts);
                        username = commandParts[2];
                        betAmount = new BigInteger(commandParts[1]);
                        balance = getBalance(username);
                        if (balance.compareTo(betAmount) < 0) {
                            out.println("insufficient amount");
                        } else {
                            out.println(username +" - Bet of " + betAmount + " placed.");
                        }
                        break;
                    case "deal":
                        username = commandParts[2];
                        String side = commandParts[1];
                        balance = getBalance(username);
                        String result = dealCards(side);
                        System.out.println(result);
                        if (result.contains("wins")) {
                            if ((side.equals("B") && result.contains("Banker wins")) 
                                || (side.equals("P") && result.contains("Player wins"))) {
                                //balance += betAmount * 2;
                                balance = balance.add(betAmount);
                                System.out.println(balance);
                                updateBalance(username, balance);
                                out.println("Bet won. Balance updated: " + balance);
                            } else {
                                balance = balance.subtract(betAmount);
                                out.println("Bet lost. Balance remains: " + balance);
                                System.out.println(balance);
                                updateBalance(username, balance);
                            }
                        } else {
                            out.println("It's a draw. Bet refunded.");
                            System.out.println(balance);
                        }
                        break;
                    case "exit":
                        System.exit(1);
                    default:
                        out.println("Unknown command: " + command);
                        break;
                }

                if (gameHistory.size() == 6) {
                    writeGameHistory(gameHistory);
                    gameHistory.clear();
                }
            }
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        }
    }

    private List<String> loadCards() {
        List<String> cards = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("cards.db"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                cards.add(line);
            }
        } catch (IOException e) {
            System.out.println("Error loading cards: " + e.getMessage());
        }
        return cards;
    }

    private String dealCards(String side) {

        if (cards.size() < 4) {
            return "Not enough cards to deal.";
        }

        int playerSum = 0;
        int bankerSum = 0;
        List<String> playerCards = new ArrayList<>();
        List<String> bankerCards = new ArrayList<>();

        // Draw initial two cards for Player and Banker
        for (int i = 0; i < 2; i++) {
            String playerCard = cards.remove(0);
            playerCards.add(playerCard);
            playerSum += getCardValue(playerCard);

            String bankerCard = cards.remove(0);
            bankerCards.add(bankerCard);
            bankerSum += getCardValue(bankerCard);
        }

        // Draw third card if necessary (sum <= 15)
        if (playerSum <= 15) {
            String playerCard = cards.remove(0);
            playerCards.add(playerCard);
            playerSum += getCardValue(playerCard);
        }

        if (bankerSum <= 15) {
            String bankerCard = cards.remove(0);
            bankerCards.add(bankerCard);
            bankerSum += getCardValue(bankerCard);
        }

        // Determine result
        String result;
        System.out.println("playerSum > " + playerSum);
        System.out.println("bankerSum > " + bankerSum);
        int[] milestones = {10, 20}; // Array to store milestone values

        if(playerSum >= 10 && playerSum <= 20){
            System.out.println("P more than 10 and less than 20");
            playerSum = playerSum - milestones[0];
        }

        if(playerSum > 20){
            System.out.println("P more than 20");
            playerSum = playerSum - milestones[1];
        }

        if(bankerSum  >= 10 && bankerSum  <= 20){
            System.out.println("B more than 10 and less than 20");
            bankerSum = bankerSum - milestones[0];
        }

        if(bankerSum > 20){
            System.out.println("B more than 20");
            bankerSum = bankerSum - milestones[1];
        } 
        System.out.println("aft playerSum > " + playerSum);
        System.out.println("aft bankerSum > " + bankerSum);

        if (playerSum > bankerSum) {
            result = "Player wins with " + playerSum + " points.";
        } else if (bankerSum > playerSum) {
            result = "Banker wins with " + bankerSum + " points.";
        } else {
            result = "Draw";
        }

        if (result.contains("Banker wins")) {
            gameHistory.add("B");
        } if(result.contains("Player wins")) {
            gameHistory.add("P");
        }else{
            gameHistory.add("D");
        }

        // Save the updated cards list back to "cards.db"
        saveCards();

        // Construct response string
        String playerCardsString = "P|" + String.join("|", playerCards);
        String bankerCardsString = "B|" + String.join("|", bankerCards);

        return playerCardsString + "," + bankerCardsString + " - " + result;
    }

    private static void writeGameHistory(List<String> gameHistory) {
        try (FileWriter csvWriter = new FileWriter("game_history.csv", true)) {
            csvWriter.append(String.join(",", gameHistory));
            csvWriter.append("\n");
        } catch (IOException e) {
            System.out.println("Error writing game history: " + e.getMessage());
        }
    }

    private static void emptyGameHistory(){
        try (FileWriter fileWriter = new FileWriter("game_history.csv", false)) {
            // Opening the file in overwrite mode without writing anything will empty it
            fileWriter.write(""); // Optional, file will still be empty
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getCardValue(String card) {
        int value = Integer.parseInt(card.split("\\.")[0]);
        return value;
    }

    private void saveCards() {
        // Save the shuffled cards to a data file named "cards.db"
        try (FileWriter writer = new FileWriter("cards.db")) {
            for (String card : cards) {
                writer.write(card + "\n");
            }
            System.out.println("Shuffled cards saved to cards.db");
        } catch (IOException e) {
            System.out.println("Error writing to cards.db: " + e.getMessage());
        }
    }

    private BigInteger getBalance(String username) {
        try (BufferedReader reader = new BufferedReader(new FileReader(username + ".db"))) {
            return new BigInteger(reader.readLine());
        } catch (IOException e) {
            System.out.println("Error reading balance for user " + username + ": " + e.getMessage());
            return BigInteger.ZERO;
        }
    }

    private void updateBalance(String username, BigInteger balance) {
        try (FileWriter writer = new FileWriter(username + ".db")) {
            writer.write(String.valueOf(balance));
        } catch (IOException e) {
            System.out.println("Error updating balance for user " + username + ": " + e.getMessage());
        }
    }
}