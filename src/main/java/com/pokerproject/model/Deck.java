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

    public synchronized List<Card> dealCards(int count) {
        List<Card> dealtCards = new ArrayList<>(count);
        for (int i = 0; i < count && !cards.isEmpty(); i++) {
            dealtCards.add(dealCard());
        }
        return dealtCards;
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

    public synchronized int remainingCards() {
        return cards.size();
    }

    public synchronized void reset() {
        initializeDeck();
    }

    public synchronized void returnCard(Card card) {
        if (card != null) {
            cards.add(card);
        }
    }

    @Override
    public String toString() {
        return "Deck: " + cards.size() + " cards remaining";
    }

    public synchronized List<Card> dealCommunityCards(int count) {
        return dealCards(count);
    }

    public synchronized boolean isEmpty() {
        return cards.isEmpty();
    }

    public synchronized Card burnCard() {
        return dealCard();
    }
}
