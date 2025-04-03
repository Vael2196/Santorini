package Frame;

import GameEntities.Builder;

// Each Tile represents one space on the board.
public class Tile {
    private int row;
    private int col;
    private int level;
    private boolean dome;
    private Builder occupant;

    /**
     * constructor method, generated through Alt+Insert (so are almost all the getters and setters), and put the default value in
     * @param row
     * @param col
     */
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

    /**
     * I wanted to play around a bit with the classes, and how we process the buildings. I was wondering whether they are
     * a part of tiles or something completely separate that we can store as a list on a tile, the length of the list representing the level of the house.
     * Ultimately, I decided against the latter since it seems more convoluted (and unnecessarily so), and also didn't put enough time to test both.
     * Then I decided to leave some of the functions here, since I was trying to account for the things I might use in the future.
     */
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
