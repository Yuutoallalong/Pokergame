package com.pokerproject.common;

import java.util.ArrayList;
import java.util.List;

import com.pokerproject.server.ClientHandler;

public class Player {
    private final String name;
    private final List<Card> hand;
    private int chips;
    private transient ClientHandler handler;
    private GameRole role;
    private boolean folded;

    public Player(String name, ClientHandler handler) {
        this.name = name;
        this.handler = handler;
        this.role = GameRole.NONE;
        this.chips = 1000;
        this.hand = new ArrayList<>();
        this.folded = false;
    }

    public String getName() {
        return name;
    }

    public ClientHandler getHandler() {
        return handler;
    }

    public GameRole getRole() {
        return role;
    }

    public void setRole(GameRole role) {
        this.role = role;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public void clearHand() {
        hand.clear();
    }

    public int getChips() {
        return chips;
    }

    public void setChips(int chips) {
        this.chips = chips;
    }

    public void addChips(int amount) {
        this.chips += amount;
    }

    public boolean deductChips(int amount) {
        if (chips >= amount) {
            chips -= amount;
            return true;
        }
        return false;
    }

    public boolean isFolded() {
        return folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    @Override
    public String toString() {
        return "Player{" +
            "name='" + name + '\'' +
            ", chips=" + chips +
            ", role=" + role +
            ", folded=" + folded +
            ", hand=" + hand +
            '}';
    }

}
