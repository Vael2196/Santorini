package Frame;

import GameEntities.*;

// Each Tile represents one space on the board.
public class Tile {
    private final int x, y;
    private Building building;
    private Worker occupant;

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
        this.building = new Building();
        this.occupant = null;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Building getBuilding() {
        return building;
    }

    public Worker getOccupant() {
        return occupant;
    }

    public boolean isOccupied() {
        return occupant != null;
    }

    public void setOccupant(Worker w) {
        this.occupant = w;
    }

    public void removeOccupant() {
        this.occupant = null;
    }

}