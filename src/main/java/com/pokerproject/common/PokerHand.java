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
    private HandType handType;
    private List<Rank> rankOrder;

    public PokerHand(List<Card> cards) {
        // should only use 5 card to check wining conditions
        if (cards.size() != 5) {
            throw new IllegalArgumentException("A poker hand must contain 5 cards");
        }
        this.cards = new ArrayList<>(cards);
        evaluateHand(); // determine handtype
    }

    private void evaluateHand() {
        // sort high to low rank
        Collections.sort(cards, Collections.reverseOrder());

        rankOrder = new ArrayList<>();
        for (Card card : cards) {
            rankOrder.add(card.getRank());
        }

        // check handtype from highest
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
    }

    // implement
    private boolean isRoyalFlush() {
        return isStraightFlush() && cards.get(0).getRank() == Rank.ACE;
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

    private boolean isFullHouse() {
        Map<Rank, Integer> rankCount = getRankCounts();

        Rank threeOfAKindRank = null;
        Rank pairRank = null;

        for (Map.Entry<Rank, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 3) {
                threeOfAKindRank = entry.getKey();
            } else if (entry.getValue() == 2) {
                pairRank = entry.getKey();
            }
        }

        if (threeOfAKindRank != null && pairRank != null) {
            rankOrder = new ArrayList<>();
            rankOrder.add(threeOfAKindRank);
            rankOrder.add(pairRank);
            return true;
        }

        return false;
    }

    private boolean isFlush() {
        Suit suit = cards.get(0).getSuit();
        for (int i = 1; i < cards.size(); i++) {
            if (cards.get(i).getSuit() != suit) {
                return false;
            }
        }

        return true;
    }

    private boolean isStraight() {
        for (int i = 0; i < cards.size() - 1; i++) {
            if (cards.get(i).getRank().getValue() != cards.get(i + 1).getRank().getValue() + 1) {
                return false;
            }
        }
        return true;
    }

    private boolean isThreeOfAKind() {
        Map<Rank, Integer> rankCount = getRankCounts();
        
        for (Map.Entry<Rank, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 3) {
                
                rankOrder = new ArrayList<>();
                rankOrder.add(entry.getKey()); 

                List<Rank> kickers = new ArrayList<>();
                for (Card card : cards) {
                    if (card.getRank() != entry.getKey()) {
                        kickers.add(card.getRank());
                    }
                }
                Collections.sort(kickers, Collections.reverseOrder());
                rankOrder.addAll(kickers);
                
                return true;
            }
        }
        return false;
    }

    private boolean isTwoPair() {
        Map<Rank, Integer> rankCount = getRankCounts();
        
        List<Rank> pairs = new ArrayList<>();
        Rank kicker = null;
        
        for (Map.Entry<Rank, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 2) {
                pairs.add(entry.getKey());
            } else if (entry.getValue() == 1) {
                kicker = entry.getKey();
            }
        }
        
        if (pairs.size() == 2) {
            Collections.sort(pairs, Collections.reverseOrder());
            rankOrder = new ArrayList<>();
            rankOrder.addAll(pairs);
            rankOrder.add(kicker);
            
            return true;
        }
        
        return false;
    }

    private boolean isPair() {
        Map<Rank, Integer> rankCount = getRankCounts();
        
        for (Map.Entry<Rank, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 2) {
                rankOrder = new ArrayList<>();
                rankOrder.add(entry.getKey());
                
                List<Rank> kickers = new ArrayList<>();
                for (Card card : cards) {
                    if (card.getRank() != entry.getKey()) {
                        kickers.add(card.getRank());
                    }
                }
                Collections.sort(kickers, Collections.reverseOrder());
                rankOrder.addAll(kickers);
                
                return true;
            }
        }
        return false;
    }

    // implement getRankCounts
    private Map<Rank, Integer> getRankCounts() {
        Map<Rank, Integer> rankCount = new HashMap<>();

        for (Card card : cards) {
            Rank rank = card.getRank();
            rankCount.put(rank, rankCount.getOrDefault(rank, 0) + 1);
        }
        return rankCount;
    }

    public HandType getHandtype() {
        return handType;
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    @Override
    public int compareTo(PokerHand other) {
        int typeComparison = Integer.compare(this.handType.getValue(), other.handType.getValue());
        if (typeComparison != 0) {
            return typeComparison;
        }

        // if same handtype
        for (int i = 0; i < Math.min(this.rankOrder.size(), other.rankOrder.size()); i++) {
            int rankComparison = Integer.compare(this.rankOrder.get(i).getValue(), other.rankOrder.get(i).getValue());
            if (rankComparison != 0) {
                return rankComparison;
            }
        }

        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(handType.getName()).append(": ");

        for (Card card : cards) {
            sb.append(card).append(" ");
        }

        return sb.toString().trim();
    }
}
