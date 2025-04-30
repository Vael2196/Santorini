package GameEntities.Gods;

import Frame.*;
import GameEntities.*;

public class Artemis extends GodCard {
    private Tile firstMove;

    @Override
    public void applyMovePower(Worker w, Tile target) {
        if (firstMove == null) {
            firstMove = w.getCurrentTile();
        } else {
            if (target.equals(firstMove)) {
                throw new IllegalArgumentException("Artemis cannot move back to origin");
            }
            firstMove = null;
        }
    }

}