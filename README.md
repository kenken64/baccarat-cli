## Baccarat CLI

```
make run-server
```

```
make run-client
```

## Workshop: Developing a Client-Server Application for a Baccarat Game

## Task 01 

Set up the project structure:

Create src and classes directories for your application.
Define a Java package named sg.edu.nus.iss.baccarat.
Create sub-packages within the main package named client and server.
Implement command-line arguments for the server program:

```
java -cp classes sg.edu.nus.iss.baccarat.server.ServerApp 12345 4
```
The first argument is the port number for the server.
The second argument is the number of decks to be used.
Shuffle the specified number of decks and save to cards.db:

Store card suits and ranks in the following format:
Suits are represented by numbers:
1.1 for Ace of Hearts, 1.2 for Ace of Diamonds, etc.
11.1 for Jack of Hearts, etc.
Example:

```
1.1
4.1
5.2
11.1
10.3
11.4
```
### (25 Marks)

## Task 02 

Develop a client program to connect with the server:


```
java -cp classes sg.edu.nus.iss.baccarat.client.ClientApp localhost:12345
```
Enable user commands in the client program:

Write server-side logic in a class named BaccaratEngine.java.
Command sequence:

```
Client Command: Login kenneth 100

Send: login|kenneth|100 to the server.
Server: Create file kenneth.db with the value 100.
Client Command: Bet 50

Send: bet|50 to the server.
Server: Handle the bet.
Client Command: Deal B or Deal P

Send: deal|B or deal|P to the server.
Server: Determine outcome, e.g., Banker wins with 7 points.
```
Implement game rules and results:

Calculate and send the outcome to the client in the format:
css
```
P|1|10|3, B|10|10|7
```
Handle betting, payouts, and specific Baccarat rules, such as the "6-Card Rule" and "Tie" bet scenarios. Update the player balance in kenneth.db accordingly.

Handle insufficient balance scenarios by notifying the client.

### (35 Marks)

## Task 03
Create a CSV file (game_history.csv) to keep track of winning games.

Each row should record up to 6 games, allowing multiple rows.
Record outcomes such as:
css

```
B,P,P,P,B,B
B,B,D,P,B,B
```
Handle tie games and record the results accurately.

### (15 Marks)

## Task 04 (Optional)
Write at least two test cases for BaccaratEngine.java to verify different game outcomes such as player wins, banker wins, and tie games.
### (5 Marks)

## Task 05
Create an HTML table that reads from game_history.csv and displays the game outcomes:

Use JavaScript to read the CSV file and generate an HTML table dynamically.
Place index.html, script.js, and game_history.csv in the same directory.
Publish the website to Vercel.

### (25 Marks)

Solution Approach

Follow the structured tasks to develop the client-server Baccarat game.
If you need specific code examples or further clarification, feel free to ask!
