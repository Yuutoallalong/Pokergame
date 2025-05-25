package src.main.java.com.pokerproject.common;

import java.util.*;

public class PokerHand implements Comparable<PokerHand> {

    public enum HandType {
        HIGH_CARD(1, "High Card"),
        PAIR(2, "Pair"),
        TWO_PAIR(3, "Two Pair"),
        THREE_OF_A_KIND(4, "Three of a Kind"),
        STRAIGHT(5, "Straight"),
        FLUSH(6, "Flush"),
        FULL_HOUSE(7, "Full House"),
        FOUR_OF_A_KIND(8, "Four of a Kind"),
        STRAIGHT_FLUSH(9, "Straight Flush"),
        ROYAL_FLUSH(10, "Royal Flush");

        private final int value;
        private final String name;

        HandType(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
    }

    private final List<Card> cards;
    private HandType handtype;
    private List<Rank> rankOrder;

    public PokerHand(List<Card> cards) {
        this.cards = new ArrayList<>(cards);
        evaluateHand(); // determine handtype
    }

    private void evaluateHand() {
        //sort high to low rank
        Collections.sort(cards, Collections.reverseOrder());

        rankOrder = new ArrayList<>();
        for (Card card : cards) {
            rankOrder.add(card.getRank());
        }

        //check handtype from highest
        if (isRoyalFlush()) {
            handType = HandType.ROYAL_FLUSH;
        } else if (isStraightFlush()) {
            handType = HandType.STRAIGHT_FLUSH;
        } else if (isFourOfAKind()) {
            handType = HandType.FOUR_OF_A_KIND;
        } else if (isFullHouse()) {
            handType = HandType.FULL_HOUSE;
        } else if (isFlush()) {
            handType = HandType.FLUSH;
        } else if (isStraight()) {
            handType = HandType.STRAIGHT;
        } else if (isThreeOfAKind()) {
            handType = HandType.THREE_OF_A_KIND;
        } else if (isTwoPair()) {
            handType = HandType.TWO_PAIR;
        } else if (isPair()) {
            handType = HandType.PAIR;
        } else {
            handType = HandType.HIGH_CARD;
        }

        //implement
        private boolean isRoyalFlush() {
            return isStraightFlush() && cards.get(0).getRank == Rank.ACE;
        }

        private boolean isStraightFlush() {
            return isStraight() && isFlush();
        }

        private boolean isFourOfAKind() {
            Map<Rank, Integer> rankCount = getRankCounts();
            
            for (Map.Entry<Rank, Integer> entry : rankCount.entrySet()) {
                if (entry.getValue() == 4) {
                    
                    rankOrder = new ArrayList<>();
                    rankOrder.add(entry.getKey());
                    
                    for (Card card : cards) {
                        if (card.getRank() != entry.getKey()) {
                            rankOrder.add(card.getRank());
                            break;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

    }
}
