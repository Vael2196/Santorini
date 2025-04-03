package Frame;

/**
 * A dummy of the board. My idea is to split it up eventually into Board being abstract,
 * the current code is gonna move to RectangleBoard, and I leave the rest up to your imagination
 */
class Board {
    public static final int SIZE = 5;
    private Tile[][] grid;

    public Board() {
        grid = new Tile[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                grid[row][col] = new Tile(row, col);
            }
        }
    }

    public Tile getCell(int row, int col) {
        if (row >= 0 && row < SIZE && col >= 0 && col < SIZE) {
            return grid[row][col];
        }
        return null;
    }
}
