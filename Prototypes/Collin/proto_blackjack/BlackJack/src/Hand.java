import java.util.ArrayList;
import java.util.Objects;

public class Hand {

    private ArrayList<Card> cards;
    private String name;

    public Hand(String newName) {
        cards = new ArrayList<>();
        name = newName;
    }

    public void reset() {
        cards = new ArrayList<>();
    }

    public void hitMe(Card card) {
        cards.add(card);
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotal(String pos) {
        int total = 0;
        for (Card card : cards) {
            if (Objects.equals(card.getPos(), pos)) {
                total += card.getValues()[1];
            } else {
                total += card.getValues()[0];
            }
        }
        if (total > 21) {
            total = getTotal("A");
        }
        return total;
    }
}
