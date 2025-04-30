package GameEntities.Gods;

import Frame.Tile;
import GameEntities.Worker;

public abstract class GodCard implements GodCardInterface {

    @Override
    public void applyMovePower(Worker w, Tile target) {
        GodCardInterface.super.applyMovePower(w, target);
    }

    @Override
    public void applyBuildPower(Worker w, Tile target) {
        GodCardInterface.super.applyBuildPower(w, target);
    }
}
