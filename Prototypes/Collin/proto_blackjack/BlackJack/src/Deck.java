import java.util.Arrays;
import java.util.Random;

public class Deck {

    private Card[] cardDeck;
    private int counter;

    public Deck() {
        createDeck();
    }

    public void shuffle() {
        Random rand = new Random();
        for (int i = 0; i < cardDeck.length; i++) {
            int randInt = rand.nextInt(cardDeck.length);
            Card tempCard = cardDeck[randInt];
            cardDeck[randInt] = cardDeck[i];
            cardDeck[i] = tempCard;
        }
        resetCounter();
    }

    private void createDeck() {
        cardDeck = new Card[52];
        Suit current = Suit.DIAMONDS;
        for (int i = 0; i < 4; i++) {
            String[] pos = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
            for (int j = 0; j < 13; j++) {
                if (j == 0) {
                    cardDeck[i] = new Card(current, pos[j], 11, 1);
                } else {
                    cardDeck[i] = new Card(current, pos[j], (Math.min(j + 1, 10)));
                }
            }

            switch (i) {
                case 1:
                    current = Suit.CLUBS;
                    break;
                case 2:
                    current = Suit.HEARTS;
                    break;
                case 3:
                    current = Suit.SPADES;
                    break;
            }

        }
        shuffle();
    }

    public Card nextCard() {
        Card out = cardDeck[counter];
        incCounter();
        return out;
    }

    public void incCounter() {
        counter++;
    }

    public void resetCounter() {
        counter = 0;
    }
}
