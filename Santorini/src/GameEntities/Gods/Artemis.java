package GameEntities.Gods;

import Frame.*;
import GameEntities.*;

public class Artemis implements GodPower {
    private Tile moveOrigin;
    private int movesMade = 0;
    private static final int MAX_MOVES = 2;

    @Override
    public String getName() {
        return "Artemis";
    }

    @Override
    public String getDescription() {
        return "May move one extra time (not back to start)";
    }

    @Override
    public void beforeMove(Worker w, Tile dest) {
        if (movesMade >= MAX_MOVES) {
            throw new IllegalArgumentException("No more moves allowed this turn");
        }
        if (movesMade == 1 && dest.equals(moveOrigin)) {
            throw new IllegalArgumentException("Artemis cannot move back to original tile");
        }
    }

    @Override
    public void afterMove(Worker w, Tile origin, Tile dest) {
        if (movesMade == 0) {
            moveOrigin = origin;
        }
        movesMade++;
    }

    @Override
    public void beforeBuild(Worker w, Tile dest) {

    }

    @Override
    public void afterBuild (Worker w, Tile dest) {

    }

    @Override
    public void resetTurn() {
        movesMade = 0;
        moveOrigin = null;
    }

    @Override
    public boolean canMoveAgain() {
        return movesMade < MAX_MOVES;
    }

}