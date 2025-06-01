package Frame.Memento;

import Frame.Boards.Board;
import Frame.Tile;
import GameEntities.Player;
import GameEntities.Worker;
import GameEntities.Gods.*;

import java.util.List;

/**
 * The “Originator” in Memento pattern. Knows how to take a snapshot of
 * the current controller+board+players and restore them.
 */
public class GameOriginator {
    private final Board board;
    private final List<Player> players;
    private final int currentPlayerIndex;

    public GameOriginator(Board board, List<Player> players, int currentPlayerIndex) {
        this.board = board;
        this.players = players;
        this.currentPlayerIndex = currentPlayerIndex;
    }

    /**
     * Create a new GameMemento capturing the current state.
     * @return
     */
    public GameMemento save() {
        GameMemento m = new GameMemento();
        m.setCurrentPlayerIndex(currentPlayerIndex);

        for (Player p : players) {
            m.addPlayer(p.getColor(), p.getGodPower().getName());
        }

        for (Player p : players) {
            for (Worker w : p.getWorkers()) {
                Tile t = w.getCurrentTile();
                String line = p.getColor() + ":" + t.getX() + "," + t.getY();
                m.addWorkerLine(line);
            }
        }

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Tile tile = board.getTile(r, c);
                int level = tile.getBuilding().getLevel();
                int dome  = tile.getBuilding().hasDome() ? 1 : 0;
                String occupant = tile.isOccupied()
                        ? tile.getOccupant().getPlayer().getColor()
                        : "none";
                String line = r + "," + c + ":" + level + "," + dome + "," + occupant;
                m.addTileLine(line);
            }
        }

        return m;
    }

    /**
     * Restore board + players from given memento.
     * Returns an array: { restoredPlayers, restoredCurrentPlayerIndex }
     */
    public static Object[] restore(GameMemento m) throws Exception {
        List<Player> restoredPlayers = new java.util.ArrayList<>();
        for (int i = 0; i < m.getNumPlayers(); i++) {
            String color = m.getPlayerColor(i);
            String godName = m.getPlayerGodName(i);

            Player p = new Player(color);
            GodPower gp;
            switch (godName) {
                case "Artemis" -> gp = new Artemis();
                case "Demeter" -> gp = new Demeter();
                case "Triton"  -> gp = new Triton();
                default -> throw new IllegalArgumentException("Unknown god: " + godName);
            }
            p.setGodPower(gp);
            restoredPlayers.add(p);
        }

        Board board = Board.createSquare(5);

        for (String wline : m.getWorkerLines()) {
            String[] parts = wline.split(":");
            String color = parts[0];
            String[] coords = parts[1].split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            Player owner = restoredPlayers.stream()
                    .filter(p -> p.getColor().equals(color))
                    .findFirst()
                    .orElseThrow();
            Worker w = new Worker(owner, board.getTile(x, y));
            owner.addWorker(w);
        }

        for (String tline : m.getTileLines()) {
            String[] parts = tline.split(":");
            String[] rc = parts[0].split(",");
            int r = Integer.parseInt(rc[0]);
            int c = Integer.parseInt(rc[1]);
            String[] props = parts[1].split(",");
            int level = Integer.parseInt(props[0]);
            boolean dome = props[1].equals("1");
            String occupantColor = props[2];

            Tile tile = board.getTile(r, c);
            for (int i = 0; i < level; i++) {
                tile.getBuilding().build();
            }
            if (dome && level == 3) {
                tile.getBuilding().build();
            }

        }

        return new Object[]{ board, restoredPlayers, m.getCurrentPlayerIndex() };
    }
}