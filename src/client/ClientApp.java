package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientApp {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -cp classes client.ClientApp <server_address>:<port>");
            return;
        }

        String[] addressParts = args[0].split(":");
        String serverAddress = addressParts[0];
        int port;

        try {
            port = Integer.parseInt(addressParts[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number.");
            return;
        }

        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server at " + serverAddress + ":" + port);
            
            while (true) {
                System.out.print("Enter command: ");
                String userCommand = scanner.nextLine();
                if(userCommand == "exit"){
                    System.out.println("exiting..");
                    break;
                }
                out.println(userCommand);
                
                String response = in.readLine();
                System.out.println("Server response: " + response);
            }
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
    }

    
}
