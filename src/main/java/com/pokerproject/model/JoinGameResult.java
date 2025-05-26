package com.pokerproject.model;
import com.pokerproject.game.Game;
public class JoinGameResult {
    private Game game;
    private String error;

    public JoinGameResult(Game game, String error) {
        this.game = game;
        this.error = error;
    }

    public Game getGame() {
        return game;
    }

    public String getError() {
        return error;
    }
}
