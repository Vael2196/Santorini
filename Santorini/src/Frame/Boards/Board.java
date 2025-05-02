package Frame.Boards;

import Frame.Tile;

/**
 * A dummy of the board
 */
public abstract class Board {
    protected final Tile[][] tiles;

    public Board(int rows, int cols) {
        tiles = new Tile[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                tiles[r][c] = new Tile(r, c);
    }

    public Tile getTile(int r, int c) {
        if (r < 0 || c < 0 || r >= tiles.length || c >= tiles[0].length) return null;
        return tiles[r][c];
    }

    public int getRows() {
        return tiles.length;
    }
    public int getCols() {
        return tiles[0].length;
    }

    /** Put factory methods for different board dimensions here (like a pentagon, a ring or whatever)*/
    public static Board createSquare(int size) {
        return new SquareBoard(size);
    }

}