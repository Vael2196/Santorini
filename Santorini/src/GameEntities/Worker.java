package GameEntities;

import Frame.Tile;

/**
 * This class doesn't work yet (in a way that there are no classes that make an instance of it within the game frame)
 * I didn't have enough imagination to represent a builder and put it onto a tile
 * (I did make it so that the tiles have a reference to an occupant,
 * but I can't quite wrap my head around the visual part of it just yet)
 */
public class Worker {
    private Tile currentTile;
    private final Player player;

    public Worker(Player player, Tile startTile) {
        this.player = player;
        this.currentTile = startTile;
        startTile.setOccupant(this);
    }

    public Player getPlayer() { return player; }
    public Tile getCurrentTile() { return currentTile; }

    /** Moves to a new tile if possible **/
    public void moveTo(Tile target) {
        currentTile.removeOccupant();
        target.setOccupant(this);
        currentTile = target;
    }

    /** Builds on an adjacent tile **/
    public void build(Tile target) {
        target.getBuilding().build();
    }
}