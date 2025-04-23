package GameEntities.Gods;

import GameEntities.*;
import Frame.*;

/**
 * Call before or/and after move and build phases
 **/
public interface GodCard {

    default void applyMovePower(Worker w, Tile target) {}
    default void applyBuildPower(Worker w, Tile target) {}

}
