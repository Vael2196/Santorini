package Frame.Memento;

import java.util.ArrayList;
import java.util.List;

/**
 * A “Memento” for storing all needed state to restore a Santorini game.
 */
public class GameMemento {
    // Index of current player (whose turn it is), 0-base
    private int currentPlayerIndex;

    private final List<String> playerColors = new ArrayList<>();
    private final List<String> playerGodNames = new ArrayList<>();

    private final List<String> workerLines = new ArrayList<>();

    private final List<String> tileLines = new ArrayList<>();

    public void setCurrentPlayerIndex(int idx) {
        this.currentPlayerIndex = idx;
    }
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void addPlayer(String color, String godName) {
        playerColors.add(color);
        playerGodNames.add(godName);
    }
    public int getNumPlayers() {
        return playerColors.size();
    }
    public String getPlayerColor(int i) {
        return playerColors.get(i);
    }
    public String getPlayerGodName(int i) {
        return playerGodNames.get(i);
    }

    public void addWorkerLine(String line) {
        workerLines.add(line);
    }
    public List<String> getWorkerLines() {
        return workerLines;
    }

    public void addTileLine(String line) {
        tileLines.add(line);
    }
    public List<String> getTileLines() {
        return tileLines;
    }
}
