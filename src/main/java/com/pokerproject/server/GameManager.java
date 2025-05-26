package com.pokerproject.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.pokerproject.game.Game;
import com.pokerproject.model.JoinGameResult;
import com.pokerproject.model.Player;

public class GameManager {
    private static GameManager instance;
    private final Map<String, Game> games = new HashMap<>();
    private final Random random = new Random();

    private GameManager() {}

    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    private String generateRandomGameId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }


    public synchronized Game createGame(Player player) {
        String gameId;
        do {
            gameId = generateRandomGameId();
        } while (games.containsKey(gameId));

        Game game = new Game(gameId,50,100);
        game.addPlayer(player);
        games.put(gameId, game);
        System.out.println("Created new game with random ID: " + gameId);
        return game;
    }

    public synchronized JoinGameResult joinGame(String gameId, Player player) {
        Game game = games.get(gameId);
        if (game == null) {
            return new JoinGameResult(null, "Failed to join game with id: " + gameId + " (ID may not exist).");
        }

        boolean added = game.addPlayer(player);
        if (added) {
            System.out.println("Player joined game: " + gameId);
            return new JoinGameResult(game, null);
        } else {
            System.out.println("Game is full: " + gameId);
            return new JoinGameResult(null, "Game id: " + gameId + " is full.");
        }
    }

    public synchronized void removeGame(String gameId) {
        games.remove(gameId);
        System.out.println("Game removed: " + gameId);
    }

    public synchronized Game getGame(String gameId) {
        return games.get(gameId);
    }
}
