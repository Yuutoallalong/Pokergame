package com.pokerproject.common;

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
