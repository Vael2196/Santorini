package Frame;

import GameEntities.*;

// Each Tile represents one space on the board.
public class Tile {
    private final int x, y;
    private Building building;
    private Worker occupant;
    private boolean isSelected;

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
        this.building = new Building();
        this.occupant = null;
        this.isSelected = false;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
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