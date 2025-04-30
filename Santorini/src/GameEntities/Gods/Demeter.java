package GameEntities.Gods;

import Frame.*;
import GameEntities.*;

public class Demeter implements GodPower {
    private Tile firstBuildTile;
    private int buildsMade = 0;
    private static final int MAX_BUILDS = 2;

    @Override
    public String getName() {
        return "Demeter";
    }

    @Override
    public String getDescription() {
        return "May build one extra time (not on same space)";
    }

    @Override
    public void beforeMove(Worker w, Tile dest) {

    }

    @Override
    public void afterMove (Worker w, Tile origin, Tile dest) {

    }

    @Override
    public void beforeBuild(Worker w, Tile dest) {
        if (buildsMade >= MAX_BUILDS) {
            throw new IllegalArgumentException("No more builds allowed this turn");
        }
        if (buildsMade == 1 && dest.equals(firstBuildTile)) {
            throw new IllegalArgumentException("Demeter cannot build twice on the same tile");
        }
    }

    @Override
    public void afterBuild(Worker w, Tile dest) {
        if (buildsMade == 0) {
            firstBuildTile = dest;
        }
        buildsMade++;
    }

    @Override
    public void resetTurn() {
        buildsMade = 0;
        firstBuildTile = null;
    }

    @Override
    public boolean canBuildAgain() {
        return buildsMade < MAX_BUILDS;
    }
}