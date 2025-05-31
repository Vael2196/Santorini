package GameEntities.Gods;

import Frame.Tile;
import GameEntities.Worker;

public class Triton implements GodPower{
    private boolean extraMoveAvailable = false;

    @Override
    public String getName() {
        return "Triton";
    }

    @Override
    public String getDescription() {
        return "May move again if the origin tile and the destination tile contain a building of the same level";
    }

    @Override
    public void beforeMove(Worker w, Tile dest) {

    }

    @Override
    public void afterMove(Worker w, Tile origin, Tile dest) {
        int originLevel = origin.getBuilding().getLevel();
        int destLevel = dest.getBuilding().getLevel();
        extraMoveAvailable = (destLevel == originLevel);
    }

    @Override
    public void beforeBuild(Worker w, Tile dest) {

    }

    @Override
    public void afterBuild(Worker w, Tile dest) {

    }

    @Override
    public void resetTurn() {
        extraMoveAvailable = false;
    }

    @Override
    public boolean canMoveAgain() {
        return extraMoveAvailable;
    }

    @Override
    public boolean allowsVoluntaryStop() {
        return true;
    }

}
