package com.pokerproject.server;

public class Player {
    private String name;
    private ClientHandler handler;

    public Player(String name, ClientHandler handler) {
        this.name = name;
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public ClientHandler getHandler() {
        return handler;
    }
}
