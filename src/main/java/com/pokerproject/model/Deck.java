package com.pokerproject.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Deck {
    private List<Card> cards;
    private transient final Random random;

    public Deck() {
        this.cards = new ArrayList<>(52);
        this.random = new Random();
        initializeDeck();
    }

    private void initializeDeck() {
        cards.clear();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
    }

    public synchronized void shuffle() {
        Collections.shuffle(cards, random);
    }

    public synchronized Card dealCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.remove(0);
    }

    public synchronized void dealCardsFromPosition(List<Player> players, int startingPlayerIndex, int cardsPerPlayer) {
        if (players == null || players.isEmpty()) {
            return;
        }
        
        int playerCount = players.size();
        startingPlayerIndex = startingPlayerIndex % playerCount;

        for (Player player : players) {
            player.clearCards();
        }
        
        for (int round = 0; round < cardsPerPlayer; round++) {
            for (int i = 0; i < playerCount; i++) {
                int playerIndex = (startingPlayerIndex + i) % playerCount;
                Player player = players.get(playerIndex);
                
                Card card = dealCard();
                if (card != null) {
                    player.addCard(card);
                }
            }
        }
    }

    public synchronized void reset() {
        initializeDeck();
    }
}
