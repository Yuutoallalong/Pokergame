package com.pokerproject.model;

public class Card implements Comparable<Card>{
    private final Rank rank;
    private final Suit suit;
    
    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }

    public Rank getValue() {
        return rank;
    }

    public Suit getSuit() {
        return suit;
    }

    @Override
    public String toString() {
        return rank.toString() + suit.getSymbol();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Card card = (Card) obj;
        return rank == card.rank && suit == card.suit;
    }

    @Override
    public int hashCode() {
        return 31 * rank.hashCode() + suit.hashCode();
    }

    @Override
    public int compareTo(Card other) {
        //use to order card by rank
        return Integer.compare(this.rank.getValue(), other.rank.getValue());
    }
}