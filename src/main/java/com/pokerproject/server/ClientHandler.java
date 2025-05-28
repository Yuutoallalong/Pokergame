package com.pokerproject.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.pokerproject.model.Game;
import com.pokerproject.model.JoinGameResult;
import com.pokerproject.model.Player;

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
            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split(":");

                if (parts[0].equalsIgnoreCase("CREATE") && parts.length == 2) {
                    String playerName = parts[1];
                    if (playerName == null || playerName.trim().isEmpty()) {
                        out.println("Player name cannot be empty.");
                        out.println("");
                        continue;
                    }
                    this.player = new Player(playerName, this, true);
                    GameManager manager = GameManager.getInstance();
                    currentGame = manager.createGame(player);
                    String gameJson = gson.toJson(currentGame);
                    String gameId = currentGame.getGameId();
                    out.println("Game created successfully! Game ID: " + gameId);
                    out.println(gameJson);
                    continue;
                } else if (parts[0].equalsIgnoreCase("JOIN") && parts.length == 3) {
                    String playerName = parts[1];
                    String gameId = parts[2];
                    if (playerName == null || playerName.trim().isEmpty()) {
                        out.println("Player name cannot be empty.");
                        out.println("");
                        continue;
                    }
                    GameManager manager = GameManager.getInstance();
                    Game game = manager.getGame(gameId);
                    if (game == null) {
                        out.println("Game not found.");
                        out.println("");
                        continue;
                    }
                    if (game.isPlayerNameExists(playerName)) {
                        out.println("This name is already taken in the game.");
                        out.println("");
                        continue;
                    }
                    this.player = new Player(playerName, this, false);
                    JoinGameResult joinGameResult = manager.joinGame(gameId, player);
                    currentGame = joinGameResult.getGame();
                    if (currentGame == null) {
                        out.println(joinGameResult.getError());
                        out.println("");
                        continue;
                    }
                    // System.out.println("JOIN GAME: " + currentGame);
                    String gameJson = gson.toJson(currentGame);
                    out.println("Joined game: " + gameId);
                    out.println(gameJson);
                    broadcastToGame("UPDATE_GAME:" + gameJson);
                    continue;
                }

                // คำสั่งอื่น ๆ ต้องตรวจสอบว่า player ถูกสร้างแล้วก่อนใช้งาน
                if (player == null) {
                    out.println("You must JOIN or CREATE a game first.");
                    out.println("");
                    continue;
                }

                if (message.equalsIgnoreCase("exit")) {
                    out.println("Goodbye!");
                    break;
                } else if (message.startsWith("LEAVE_GAME:")) {
                    // System.out.println("LEAVE_GAME: " + message);
                    String[] exitParts = message.split(":", 3);
                    if (exitParts.length < 3) {
                        out.println("Invalid LEAVE_GAME command format.");
                        continue;
                    }
                    String playerName = exitParts[1];
                    String gameId = exitParts[2];

                    Game game = GameManager.getInstance().getGame(gameId);
                    if (game != null) {

                        boolean removed = game.removePlayerByName(playerName);
                        if (game.getPlayers().isEmpty()) {
                            GameManager.getInstance().removeGame(gameId);
                        }
                        if (removed) {
                            out.println("LEAVE_GAME_SUCCESS");
                            String gameJson = gson.toJson(game);
                            broadcastToOthers("UPDATE_GAME:" + gameJson, playerName);

                            // System.out.println("Player " + playerName + " left game " + gameId);
                        } else {
                            out.println("Failed to leave game - player not found");
                        }
                    } else {
                        out.println("Game not found");
                    }

                    currentGame = null;

                    continue;
                } else if (message.startsWith("START_GAME:")) {
                    String[] startParts = message.split(":", 2);
                    String gameId = startParts[1];
                    Game game = GameManager.getInstance().getGame(gameId);
                    game.setState(Game.State.PLAYING);
                    game.getDeck().shuffle();
                    game.initializeFirstDealer();
                    List<Player> players = game.getPlayers();
                    game.getDeck().dealCardsFromPosition(players, 0, 2);
                    String gameJson = gson.toJson(game);
                    broadcastToGame("UPDATE_GAME:" + gameJson);
                    continue;
                } else if (message.startsWith("FOLD:")) {
                    String[] foldParts = message.split(":", 3);
                    String gameId = foldParts[1];
                    String playerName = foldParts[2];
                    Game game = GameManager.getInstance().getGame(gameId);
                    Player actionPlayer = game.getPlayerByName(playerName);
                    game.processPlayerAction(actionPlayer, Game.Action.FOLD, 0);
                    String gameJsonFold = gson.toJson(game);
                    broadcastToGame("UPDATE_GAME:" + gameJsonFold);
                    continue;
                } else if (message.startsWith("CHECK:")) {
                    String[] foldParts = message.split(":", 3);
                    String gameId = foldParts[1];
                    String playerName = foldParts[2];
                    Game game = GameManager.getInstance().getGame(gameId);
                    Player actionPlayer = game.getPlayerByName(playerName);
                    game.processPlayerAction(actionPlayer, Game.Action.CHECK, 0);
                    String gameJsonFold = gson.toJson(game);
                    broadcastToGame("UPDATE_GAME:" + gameJsonFold);
                    continue;
                } else if (message.startsWith("CALL:")) {
                    String[] foldParts = message.split(":", 4);
                    String gameId = foldParts[1];
                    String playerName = foldParts[2];
                    String callAmountStr = foldParts[3];
                    int callAmount = Integer.parseInt(callAmountStr);
                    Game game = GameManager.getInstance().getGame(gameId);
                    Player actionPlayer = game.getPlayerByName(playerName);
                    game.processPlayerAction(actionPlayer, Game.Action.CALL, callAmount);
                    String gameJsonFold = gson.toJson(game);
                    broadcastToGame("UPDATE_GAME:" + gameJsonFold);
                    continue;
                } else if (message.startsWith("BET:")) {
                    String[] foldParts = message.split(":", 4);
                    String gameId = foldParts[1];
                    String playerName = foldParts[2];
                    String betAmountStr = foldParts[3];
                    int betAmount = Integer.parseInt(betAmountStr);
                    Game game = GameManager.getInstance().getGame(gameId);
                    Player actionPlayer = game.getPlayerByName(playerName);
                    game.processPlayerAction(actionPlayer, Game.Action.BET, betAmount);
                    String gameJsonFold = gson.toJson(game);
                    broadcastToGame("UPDATE_GAME:" + gameJsonFold);
                    continue;
                } else if (message.startsWith("RAISE:")) {
                    String[] foldParts = message.split(":", 4);
                    String gameId = foldParts[1];
                    String playerName = foldParts[2];
                    String raiseAmountStr = foldParts[3];
                    int raiseAmount = Integer.parseInt(raiseAmountStr);
                    Game game = GameManager.getInstance().getGame(gameId);
                    Player actionPlayer = game.getPlayerByName(playerName);
                    game.processPlayerAction(actionPlayer, Game.Action.RAISE, raiseAmount);
                    String gameJsonFold = gson.toJson(game);
                    broadcastToGame("UPDATE_GAME:" + gameJsonFold);
                    continue;
                } else if (message.startsWith("NEXTGAME:")) {
                    String[] foldParts = message.split(":", 3);
                    String gameId = foldParts[1];
                    String playerName = foldParts[2];
                    System.out.println("NEXT GAME TRIGGER " + gameId + " " + playerName);
                    Game game = GameManager.getInstance().getGame(gameId);
                    Player actionPlayer = game.getPlayerByName(playerName);
                    game.processPlayerAction(actionPlayer, Game.Action.NEXT, 0);
                    String gameJsonFold = gson.toJson(game);
                    broadcastToGame("UPDATE_GAME:" + gameJsonFold);
                    continue;
                } 
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // System.out.println(e);
        } finally {
            cleanup();
        }
    }

    private void broadcastToOthers(String message, String excludePlayerName) {
        if (currentGame != null) {
            for (Player p : currentGame.getPlayers()) {
                if (!p.getName().equals(excludePlayerName)) {
                    p.getHandler().sendMessage(message);
                }
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void broadcastToGame(String message) {
        if (currentGame != null) {
            // สร้าง list copy เพื่อป้องกัน ConcurrentModificationException
            List<Player> playersCopy = new ArrayList<>(currentGame.getPlayers());
            for (Player p : playersCopy) {
                try {
                    p.getHandler().sendMessage(message);
                } catch (Exception e) {
                    System.err.println("Failed to send message to player " + p.getName() + ": " + e.getMessage());
                }
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
            // System.out.println("Client disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
