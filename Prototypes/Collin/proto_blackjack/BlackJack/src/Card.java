public class Card {

    private Suit suit;
    private String pos;
    private final int[] value = new int[2];

    public Card(Suit newSuit, String newPos, int value1) {
        suit = newSuit;
        pos = newPos;
        value[0] = value1;
    }

    public Card(Suit newSuit, String newPos, int value1, int value2) {
        suit = newSuit;
        pos = newPos;
        value[0] = value1;
        value[1] = value2;
    }

    public Suit getSuit() {
        return suit;
    }

    public String getSuitString() {
        return suit + "";
    }

    public String getPos() {
        return pos;
    }

    public int[] getValues() {
        return value;
    }

    @Override
    public String toString() {
        return suit.emojiBlack() + pos;
    }
}
