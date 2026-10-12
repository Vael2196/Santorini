package online.core;

import Frame.Boards.Board;
import Frame.Tile;
import GameEntities.Player;
import GameEntities.Worker;
import GameEntities.Gods.*;
import online.protocol.GameAction;
import online.protocol.MatchSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

/** Headless state machine using the existing model and god strategies.
 * Its owner serializes actions. There are no Swing, I/O or Spring dependencies. */
public final class Match {
    public static final List<String> GODS = List.of("Artemis", "Demeter", "Triton");
    private final Board board = Board.createSquare(5);
    private final List<Player> players = new ArrayList<>();
    private final GameMode mode;
    private final Set<Integer> eliminated = new LinkedHashSet<>();
    private String phase = "PLACE";
    private int turn;
    private Integer selected;
    private Integer winner;
    private int movesMade;
    private int buildsMade;

    public Match(List<String> gods) {
        this(gods, GameMode.DUEL);
    }
    public Match(List<String> gods, GameMode mode) {
        this.mode = mode;
        if (gods.size() != mode.seats) throw new IllegalArgumentException("The number of players does not match the mode");
        for (int seat = 0; seat < mode.seats; seat++) {
            Player player = new Player(seat == 0 ? "Red" : "Blue");
            player.setGodPower(power(gods.get(seat)));
            for (int i = 0; i < mode.workers; i++) player.addWorker(new Worker(player));
            players.add(player);
        }
    }

    private static GodPower power(String name) {
        return switch (name) {
            case "Artemis" -> new Artemis();
            case "Demeter" -> new Demeter();
            case "Triton" -> new Triton();
            case "None" -> new NoPower();
            default -> throw new IllegalArgumentException("Unknown god power");
        };
    }

    public void apply(int seat, GameAction action) {
        if (winner != null) throw new IllegalArgumentException("The match has ended");
        if (seat != turn) throw new IllegalArgumentException("It is not your turn");
        if (action == null || !legalActions().contains(action)) {
            throw new IllegalArgumentException("That action is not legal in the current phase");
        }
        GodPower power = players.get(turn).getGodPower();
        switch (action.type()) {
            case "PLACE" -> {
                players.get(turn).getWorkers().stream().filter(w -> w.getCurrentTile() == null)
                        .findFirst().orElseThrow().setStartingTile(board.getTile(action.row(), action.col()));
                if (players.get(turn).getWorkers().stream().allMatch(w -> w.getCurrentTile() != null)) {
                    advancePlacement();
                }
            }
            case "SELECT" -> { selected = action.worker(); phase = "MOVE"; }
            case "CANCEL" -> { selected = null; phase = "SELECT"; }
            case "MOVE" -> {
                Worker worker = worker(selected);
                Tile origin = worker.getCurrentTile();
                Tile destination = board.getTile(action.row(), action.col());
                worker.moveTo(destination);
                power.afterMove(worker, origin, destination);
                movesMade++;
                if (origin.getBuilding().getLevel() == 2 && destination.getBuilding().getLevel() == 3) {
                    finish(turn);
                } else if (!power.canMoveAgain() || moveActions(worker).isEmpty()) phase = "BUILD";
            }
            case "END_MOVE" -> phase = "BUILD";
            case "BUILD" -> {
                Tile destination = board.getTile(action.row(), action.col());
                worker(selected).build(destination);
                power.afterBuild(worker(selected), destination);
                buildsMade++;
                if (!power.canBuildAgain() || buildActions(worker(selected)).isEmpty()) endTurn();
            }
            case "END_TURN" -> endTurn();
            default -> throw new IllegalArgumentException("Unknown action");
        }
        resolveBlocked();
    }

    public boolean active(int seat) { return seat >= 0 && seat < mode.seats && !eliminated.contains(seat); }
    public List<Integer> winners() {
        if (winner == null) return List.of();
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < mode.seats; i++) if (mode.team(i) == mode.team(winner)) result.add(i);
        return List.copyOf(result);
    }
    public void resign(int seat) {
        if (seat < 0 || seat >= mode.seats) throw new IllegalArgumentException("Invalid seat");
        if (winner != null || !active(seat)) return;
        if (mode.teams) {
            for (int i = 0; i < mode.seats; i++) if (mode.team(i) != mode.team(seat)) { finish(i); return; }
        }
        eliminated.add(seat);
        for (Worker w : players.get(seat).getWorkers()) if (w.getCurrentTile() != null) w.getCurrentTile().removeOccupant();
        if (mode.seats - eliminated.size() == 1) { finish(nextActive(seat)); return; }
        if (seat == turn) {
            resetTurn(); turn = nextActive(turn);
            if (!phase.equals("PLACE")) phase = "SELECT";
            else if (allPlaced()) { phase = "SELECT"; turn = nextActive(mode.seats - 1); }
        }
        resolveBlocked();
    }
    private boolean allPlaced() {
        for (int i = 0; i < mode.seats; i++) if (active(i) && players.get(i).getWorkers().stream().anyMatch(w -> w.getCurrentTile() == null)) return false;
        return true;
    }
    private int nextActive(int seat) {
        for (int i = 1; i <= mode.seats; i++) if (active((seat + i) % mode.seats)) return (seat + i) % mode.seats;
        throw new IllegalStateException("No active player");
    }
    private void advancePlacement() {
        turn = nextActive(turn);
        if (allPlaced()) { phase = "SELECT"; turn = nextActive(mode.seats - 1); }
    }
    private void resolveBlocked() {
        if (winner == null && !phase.equals("PLACE") && legalActions().isEmpty()) resign(turn);
    }
    private void finish(int seat) { winner = seat; phase = "FINISHED"; }
    private void resetTurn() {
        players.get(turn).getGodPower().resetTurn(); selected = null; movesMade = 0; buildsMade = 0;
    }
    private void endTurn() { resetTurn(); turn = nextActive(turn); phase = "SELECT"; }
    private Worker worker(int id) { return players.get(id / mode.workers).getWorkers().get(id % mode.workers); }

    public List<GameAction> legalActions() {
        if (winner != null) return List.of();
        List<GameAction> actions = new ArrayList<>();
        switch (phase) {
            case "PLACE" -> forEachTile(t -> {
                if (!t.isOccupied()) actions.add(GameAction.at("PLACE", t.getX(), t.getY()));
            });
            case "SELECT" -> {
                for (int index = 0; index < mode.workers; index++) {
                    if (!moveActions(players.get(turn).getWorkers().get(index)).isEmpty()) {
                        actions.add(GameAction.select(turn * mode.workers + index));
                    }
                }
            }
            case "MOVE" -> {
                actions.addAll(moveActions(worker(selected)));
                actions.add(GameAction.simple(movesMade == 0 ? "CANCEL" : "END_MOVE"));
            }
            case "BUILD" -> {
                actions.addAll(buildActions(worker(selected)));
                if (buildsMade > 0) actions.add(GameAction.simple("END_TURN"));
            }
            default -> { }
        }
        return List.copyOf(actions);
    }

    private List<GameAction> moveActions(Worker worker) {
        if (worker.getCurrentTile() == null) return List.of();
        List<GameAction> result = new ArrayList<>();
        Tile origin = worker.getCurrentTile();
        forEachTile(target -> {
            if (!adjacent(origin, target) || target.isOccupied() || target.getBuilding().hasDome()) return;
            // Preserve the original application's one-level ascent AND descent rule.
            if (Math.abs(target.getBuilding().getLevel() - origin.getBuilding().getLevel()) > 1) return;
            try {
                worker.getPlayer().getGodPower().beforeMove(worker, target);
                result.add(GameAction.at("MOVE", target.getX(), target.getY()));
            } catch (IllegalArgumentException ignored) { /* Strategy rejects this destination. */ }
        });
        return result;
    }

    private List<GameAction> buildActions(Worker worker) {
        List<GameAction> result = new ArrayList<>();
        forEachTile(target -> {
            if (!adjacent(worker.getCurrentTile(), target) || target.isOccupied() || target.getBuilding().hasDome()) return;
            try {
                worker.getPlayer().getGodPower().beforeBuild(worker, target);
                result.add(GameAction.at("BUILD", target.getX(), target.getY()));
            } catch (IllegalArgumentException ignored) { /* Strategy rejects this destination. */ }
        });
        return result;
    }

    private static boolean adjacent(Tile origin, Tile target) {
        return origin != target && Math.abs(origin.getX() - target.getX()) <= 1
                && Math.abs(origin.getY() - target.getY()) <= 1;
    }

    private void forEachTile(java.util.function.Consumer<Tile> visitor) {
        for (int row = 0; row < 5; row++) for (int col = 0; col < 5; col++) visitor.accept(board.getTile(row, col));
    }

    public MatchSnapshot snapshot() {
        List<MatchSnapshot.Cell> cells = new ArrayList<>();
        forEachTile(tile -> {
            Integer id = null;
            for (int index = 0; index < mode.seats * mode.workers; index++) if (tile.getOccupant() == worker(index)) id = index;
            cells.add(new MatchSnapshot.Cell(tile.getX(), tile.getY(), tile.getBuilding().getLevel(),
                    tile.getBuilding().hasDome(), id, id == null ? null : id / mode.workers));
        });
        return new MatchSnapshot(phase, turn, selected, winner, List.copyOf(cells), legalActions(), mode.workers, List.copyOf(eliminated), winners());
    }

    private static final class NoPower implements GodPower {
        public String getName() { return "None"; }
        public String getDescription() { return "Classic rules without god powers"; }
        public void beforeMove(Worker w, Tile t) { }
        public void afterMove(Worker w, Tile o, Tile t) { }
        public void beforeBuild(Worker w, Tile t) { }
        public void afterBuild(Worker w, Tile t) { }
        public void resetTurn() { }
    }
}
