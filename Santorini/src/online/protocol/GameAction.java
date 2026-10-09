package online.protocol;

/** Coordinates are zero-based rows/columns; worker IDs are 0..3. */
public record GameAction(String type, Integer worker, Integer row, Integer col) {
    public static GameAction at(String type, int row, int col) { return new GameAction(type, null, row, col); }
    public static GameAction select(int worker) { return new GameAction("SELECT", worker, null, null); }
    public static GameAction simple(String type) { return new GameAction(type, null, null, null); }
}
