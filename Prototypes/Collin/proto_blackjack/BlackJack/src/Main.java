public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }

    public void roundStart() {
        boolean houseWins = false;
        boolean playerWins = false;
        boolean noWinner = false;
        int blackjack = 21;
        Hand house = new Hand("House");
        Hand player = new Hand("Player");
        Deck deck = new Deck();
        house.hitMe(deck.nextCard());
        house.hitMe(deck.nextCard());
        player.hitMe(deck.nextCard());
        player.hitMe(deck.nextCard());
        //while (!houseWins && !playerWins && !noWinner) {
            boolean hit = false; //input here

            while (hit) {
                player.hitMe(deck.nextCard());
                //update view
                if (player.getTotal("none") > blackjack) {
                    houseWins = true;
                    hit = false;
                    //house wins
                } else {
                    hit = true; //input here
                }
            }
            if (!houseWins) {
                hit = house.getTotal("none") < 17;
                while (hit) {
                    house.hitMe(deck.nextCard());
                    //update view
                    if (house.getTotal("none") > blackjack) {
                        houseWins = true;
                        break;
                        //house wins
                    }
                    hit = house.getTotal("none") >= blackjack? false : true;
                }
            }

        //}
    }
}