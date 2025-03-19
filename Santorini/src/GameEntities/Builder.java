package GameEntities;

import Frame.Tile;


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