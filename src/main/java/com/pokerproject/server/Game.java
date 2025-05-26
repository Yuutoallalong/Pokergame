package com.pokerproject.server;
import java.util.ArrayList;
import java.util.List;

import com.pokerproject.common.*;


public class Game {
    private final String gameId;
    private final List<Player> players;
    private Deck deck;
    private int pot;
    private GameState state;

    private static final int MAX_PLAYERS = 4;

    public Game(String gameId) {
        this.gameId = gameId;
        this.players = new ArrayList<>();
        this.deck = new Deck();
        this.pot = 0;
        this.state = GameState.WAITING;
    }

    public boolean addPlayer(Player player) {
        if (players.size() < MAX_PLAYERS) {
            players.add(player);
            return true;
        }
        return false;
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public String getGameId() {
        return gameId;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getPot() {
        return pot;
    }

    public void addToPot(int amount) {
        this.pot += amount;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }
}
