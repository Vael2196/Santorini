package GameEntities.Gods;

import GameEntities.*;
import Frame.*;

/**
 * Call before or/and after move and build phases
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

}