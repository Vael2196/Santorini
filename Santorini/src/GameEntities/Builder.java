package GameEntities;

import Frame.Tile;

/**
 * This class doesn't work yet (in a way that there are no classes that make an instance of it within the game frame)
 * I didn't have enough imagination to represent a builder and put it onto a tile
 * (I did make it so that the tiles have a reference to an occupant,
 * but I can't quite wrap my head around the visual part of it just yet)
 */
public class Builder {
    private int row;
    private int col;
    private String player;

    public Builder(String player, int row, int col) {
        this.player = player;
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }
    
    public int getCol() {
        return col;
    }

    public String getPlayer() {
        return player;
    }


    public void moveTo(Tile from, Tile to) {
        if (from.getOccupant() != this) {
            System.out.println("Error: Builder not found at the source cell.");
            return;
        }
        from.removeOccupant();
        to.setOccupant(this);
        this.row = to.getRow();
        this.col = to.getCol();
    }
}