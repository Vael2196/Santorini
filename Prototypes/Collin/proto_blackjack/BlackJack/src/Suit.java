enum Suit {
    SPADES( "♠️" , "♤" ), HEARTS( "♥️" , "♡" ), CLUBS( "♣️" , "♧" ), DIAMONDS( "♦️" , "♢" );
    private final String emojiBlack, emojiWhite ;
    Suit( String emojiBlack , String emojiWhite ) {  // Constructor.
        this.emojiBlack = emojiBlack ;
        this.emojiWhite= emojiWhite ;
    }
    String emojiBlack() {  // Accessor (getter).
        return this.emojiBlack ;
    }
    String emojiWhite() {  // Accessor (getter).
        return this.emojiWhite ;
    }
}