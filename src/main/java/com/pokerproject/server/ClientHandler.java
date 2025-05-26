package com.pokerproject.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.google.gson.Gson;
import com.pokerproject.common.Game;
import com.pokerproject.common.JoinGameResult;
import com.pokerproject.common.Player;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Player player;
    private Game currentGame;
    private static final Gson gson = new Gson();
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String message = in.readLine();
            if (message != null) {
                String[] parts = message.split(":");
                if (parts[0].equalsIgnoreCase("CREATE") && parts.length == 2) {
                    String playerName = parts[1];
                    if (playerName == null || playerName.trim().isEmpty()) {
                        out.println("Player name cannot be empty.");
                        return;
                    }
                    this.player = new Player(playerName, this);
                    GameManager manager = GameManager.getInstance();
                    currentGame = manager.createGame(player);
                    System.out.println("CREATE GAME: "+currentGame);
                    String gameJson = gson.toJson(currentGame);
                    String gameId = currentGame.getGameId();
                    out.println("Game created successfully! Game ID: " + gameId);
                    out.println(gameJson);
                } else if (parts[0].equalsIgnoreCase("JOIN") && parts.length == 3) {
                    String playerName = parts[1];
                    String gameId = parts[2];
                    if (playerName == null || playerName.trim().isEmpty()) {
                        out.println("Player name cannot be empty.");
                        return;
                    }
                    this.player = new Player(playerName, this);
                    GameManager manager = GameManager.getInstance();

                    Game game = manager.getGame(gameId);
                    if (game.isPlayerNameExists(playerName)) {
                        out.println("This name is already taken in the game.");
                        return;
                    }
                    
                    JoinGameResult joinGameResult = manager.joinGame(gameId, player);
                    currentGame = joinGameResult.getGame();
                    System.out.println("JOIN GAME: "+currentGame);
                    if (currentGame == null) {
                        out.println(joinGameResult.getError());
                        return;
                    }
                    String gameJson = gson.toJson(currentGame);
                    out.println("Joined game: " + gameId);
                    out.println(gameJson);
                    broadcastToGame("UPDATE_GAME:"+gameJson);  
                } else {
                    out.println("Invalid command format.");
                    return;
                }
            }

            // รับข้อความในเกมเหมือนเดิม

            while ((message = in.readLine()) != null) {
                System.out.println("[" + player.getName() + "] " + message);    
                if (message.equalsIgnoreCase("exit")) {
                    out.println("Goodbye!");
                    break;
                } else if (message.startsWith("EXIT_GAME:")) {
                    System.out.println("EXIT_GAME: " + message);
                    String[] parts = message.split(":", 3);
                    String playerName = parts[1];
                    String gameId = parts[2];

                    Game game = GameManager.getInstance().getGame(gameId);
                    if (game != null) {
                        game.removePlayerByName(playerName);
                        String gameJson = gson.toJson(game);
                        broadcastToGame("UPDATE_GAME:"+gameJson);  
                    }
                } else {
                    broadcastToGame("[" + player.getName() + "]: " + message);
                }
            }
            
            

        } catch (IOException e) {
            e.printStackTrace();
        } catch(Exception e){
            System.out.println(e);
        } 
        finally {
            cleanup();
        }
    }


    public void sendMessage(String message) {
        out.println(message);
    }

    private void broadcastToGame(String message) {
        if (currentGame != null) {
            for (Player p : currentGame.getPlayers()) {
                p.getHandler().sendMessage(message);
            }
        }
    }

    private void cleanup() {
        try {
            if (currentGame != null && player != null) {
                currentGame.removePlayer(player);
                broadcastToGame(player.getName() + " has left the game.");
            }
            in.close();
            out.close();
            clientSocket.close();
            System.out.println("Client disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
