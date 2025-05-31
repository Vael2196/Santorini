package GameEntities.Gods;

import GameEntities.*;
import Frame.*;

/**
 * A skeleton that defines the common behaviour of all the God cards/powers
 * An attempt to turn the God powers into a Strategy pattern.
 **/
public interface GodPower {
    String getName();
    String getDescription();

    void beforeMove(Worker w, Tile dest);
    void afterMove (Worker w, Tile origin, Tile dest);
    void beforeBuild(Worker w, Tile dest);
    void afterBuild (Worker w, Tile dest);
    void resetTurn();

    default boolean canMoveAgain() {
        return false;
    }

    default boolean canBuildAgain() {
        return false;
    }

    default boolean allowsVoluntaryStop() { return false; }
}