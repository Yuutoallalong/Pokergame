package src.main.java.com.pokerproject.server;

import java.util.HashMap;
import java.util.Map;

public class GameManager {
    private static GameManager instance;
    private final Map<String, Game> games = new HashMap<>();

    private GameManager() {}

    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public synchronized Game joinOrCreateGame(String gameId, Player player) {
        Game game = games.get(gameId);
        if (game == null) {
            game = new Game(gameId);
            games.put(gameId, game);
            System.out.println("Created new game with ID: " + gameId);
        }

        boolean added = game.addPlayer(player);
        if (added) {
            System.out.println("Player joined game: " + gameId);
            return game;
        } else {
            System.out.println("Game is full: " + gameId);
            return null;
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
