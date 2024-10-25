package server;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

class BaccaratEngine implements Runnable {
    private Socket clientSocket;
    private List<String> cards;
    private volatile List<String> gameHistory = new ArrayList<>();

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
                        if(betAmount.compareTo(balance) < 0){
                            String result = dealCards(side);
                            if (result.contains("wins")) {
                                if ((side.equals("B") && result.contains("Banker wins")) 
                                    || (side.equals("P") && result.contains("Player wins"))) {
                                    balance = balance.add(betAmount);
                                    updateBalance(username, balance);
                                    out.println("Bet won. Balance updated: " + balance);
                                } else {
                                    balance = balance.subtract(betAmount);
                                    out.println("Bet lost. Balance remains: " + balance);
                                    updateBalance(username, balance);
                                }
                            } else {
                                out.println("It's a draw. Bet refunded.");
                            }
                        }else{
                            out.println("Insufficient betting amount");
                        }
                        
                        break;
                    case "exit":
                        System.exit(1);
                    default:
                        out.println("Unknown command: " + command);
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private synchronized List<String> loadCards() {
        List<String> cards = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("cards.db"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                cards.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error loading cards: " + e.getMessage());
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
        String result;
        int[] milestones = {10, 20}; // Array to store milestone values

        synchronized(cards){
            // Draw initial two cards for Player and Banker
            for (int i = 0; i < 2; i++) {
                String playerCard = cards.remove(0);
                playerCards.add(playerCard);
                playerSum += getCardValue(playerCard);
                String bankerCard = cards.remove(0);
                bankerCards.add(bankerCard);
                bankerSum += getCardValue(bankerCard);
            }

            
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
        
            if(playerSum >= 10 && playerSum <= 20){
                playerSum = playerSum - milestones[0];
            }

            if(playerSum > 20){
                playerSum = playerSum - milestones[1];
            }

            if(bankerSum  >= 10 && bankerSum  <= 20){
                bankerSum = bankerSum - milestones[0];
            }

            if(bankerSum > 20){
                bankerSum = bankerSum - milestones[1];
            } 
            
            resetSumIfNecessary(playerSum);
            resetSumIfNecessary(bankerSum);
            
            if (playerSum > bankerSum) {
                result = "Player wins with " + playerSum + " points.";
            } else if (bankerSum > playerSum) {
                result = "Banker wins with " + bankerSum + " points.";
            } else {
                result = "Draw";
            }
        }
        

        synchronized (gameHistory) {
            if (result.contains("Banker wins")) {
                gameHistory.add("B");
            } if(result.contains("Player wins")) {
                gameHistory.add("P");
            }else if(result.contains("Draw")){
                gameHistory.add("D");
            }

            // Write to history if game count reaches 6
            if (gameHistory.size() == 6) {
                writeGameHistory(new ArrayList<>(gameHistory));
                gameHistory.clear();
            }
        }

        
        // Save the updated cards list back to "cards.db"
        saveCards();

        // Construct response string
        String playerCardsString = "P|" + String.join("|", playerCards);
        String bankerCardsString = "B|" + String.join("|", bankerCards);

        return playerCardsString + "," + bankerCardsString + " - " + result;
    }

    private void resetSumIfNecessary(int sum) {
        if (sum == 20 || sum == 10) {
            sum = 0;
        }
    }

    private static synchronized void writeGameHistory(List<String> gameHistorySnapshot) {
        try (FileWriter csvWriter = new FileWriter("game_history.csv", true)) {
            synchronized (gameHistorySnapshot) {
                if (!gameHistorySnapshot.isEmpty()) {
                    csvWriter.append(String.join(",", gameHistorySnapshot)).append("\n");
                }
            }
        } catch (IOException e) {
            System.out.println("Error writing game history: " + e.getMessage());
        }
    }

    private synchronized int getCardValue(String card) {
        int value = Integer.parseInt(card.split("\\.")[0]);
        if(value == 11 || value == 12 || value ==13)
            value = 10;
        return value;
    }

    private synchronized void saveCards() {
        // Save the shuffled cards to a data file named "cards.db"
        try (FileWriter writer = new FileWriter("cards.db")) {
            for (String card : cards) {
                writer.write(card + "\n");
            }
            System.out.println("Shuffled cards saved to cards.db");
        } catch (IOException e) {
            System.err.println("Error writing to cards.db: " + e.getMessage());
        }
    }

    private BigInteger getBalance(String username) {
        try (BufferedReader reader = new BufferedReader(new FileReader(username + ".db"))) {
            return new BigInteger(reader.readLine());
        } catch (IOException e) {
            System.err.println("Error reading balance for user " + username + ": " + e.getMessage());
            return BigInteger.ZERO;
        }
    }

    private void updateBalance(String username, BigInteger balance) {
        try (FileWriter writer = new FileWriter(username + ".db")) {
            writer.write(String.valueOf(balance));
        } catch (IOException e) {
            System.err.println("Error updating balance for user " + username + ": " + e.getMessage());
        }
    }
}
