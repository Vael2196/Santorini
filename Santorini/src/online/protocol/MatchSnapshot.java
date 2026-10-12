package online.protocol;
import java.util.List;
public record MatchSnapshot(String phase, int turn, Integer selectedWorker, Integer winner,
                            List<Cell> cells, List<GameAction> legalActions,
                            int workersPerPlayer, List<Integer> eliminated, List<Integer> winners) {
    public MatchSnapshot(String phase, int turn, Integer selectedWorker, Integer winner, List<Cell> cells, List<GameAction> legalActions) {
        this(phase, turn, selectedWorker, winner, cells, legalActions, 2, List.of(), winner == null ? List.of() : List.of(winner));
    }
    public record Cell(int row, int col, int level, boolean dome, Integer worker, Integer owner) {}
}
