package GameEntities.Gods;

import Frame.*;
import GameEntities.*;

public class Demeter extends GodCard {
    private Tile firstBuild;

    @Override
    public void applyBuildPower(Worker w, Tile target) {
        if (firstBuild == null) {
            firstBuild = target;
        } else {
            if (target.equals(firstBuild)) {
                throw new IllegalArgumentException("Demeter cannot build twice on same tile");
            }
            firstBuild = null;
        }
    }

    @Override
    public void applyMovePower(Worker w, Tile target) {
        super.applyMovePower(w, target);
    }
}