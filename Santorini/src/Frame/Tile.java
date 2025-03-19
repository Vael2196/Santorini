package Frame;

import GameEntities.Builder;

// Each Tile represents one space on the board.
public class Tile {
    private int row;
    private int col;
    private int level;
    private boolean dome;
    private Builder occupant;

    public Tile(int row, int col) {
        this.row = row;
        this.col = col;
        this.level = 0;
        this.dome = false;
        this.occupant = null;
    }

    public int getRow() {

        return row;
    }

    public int getCol() {

        return col;
    }
    public int getLevel() {

        return level;
    }
    public boolean hasDome() {

        return dome;
    }
    public Builder getOccupant() {

        return occupant;
    }

    public void build() {
        if (dome) {
            System.out.println("Cannot build: Dome already present.");
            return;
        }
        if (level < 3) {
            level++;
        } else {
            dome = true;
        }
    }

    public void setOccupant(Builder builder) {
        this.occupant = builder;
    }

    public void removeOccupant() {
        this.occupant = null;
    }
}
