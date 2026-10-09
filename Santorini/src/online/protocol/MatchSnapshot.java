package online.protocol;
import java.util.List;
public record MatchSnapshot(String phase, int turn, Integer selectedWorker, Integer winner,
                            List<Cell> cells, List<GameAction> legalActions) {
    public record Cell(int row, int col, int level, boolean dome, Integer worker, Integer owner) {}
}
