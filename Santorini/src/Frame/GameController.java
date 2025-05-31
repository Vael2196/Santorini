package Frame;

import GameEntities.*;
import Frame.*;
import Frame.Boards.*;

import javax.swing.*;
import java.util.List;

/**
 * Controller level class that contains the core game logic for a game of Santorini (no GodPowers included)
 */
public class GameController implements Controller {

    public enum Phase {SETUP, SELECT, MOVE, BUILD}

    private final Board board;
    private GameFrame frame;
    private final List<Player> players;
    private int currentPlayerIndex = 0;
    private Phase phase;
    private Worker selectedWorker;

    /**
     * Constructor method
     * @param board
     * @param frame
     * @param players
     */
    public GameController(Board board, GameFrame frame, List<Player> players) {
        this.board = board;
        this.frame = frame;
        this.players = players;
        phase = Phase.SETUP;
        //announcePhase();
    }

    @Override
    public void setFrame(GameFrame frame) {
        this.frame = frame;
    }

    @Override
    public void onTileClicked(Tile tile) {
        Player current = players.get(currentPlayerIndex);
        switch (phase) {
            case SETUP:
                handleSetup(current, tile);
                break;
            case SELECT:
                handleSelect(current, tile);
                break;
            case MOVE:
                handleMove(current, tile);
                break;
            case BUILD:
                handleBuild(current, tile);
                break;
        }
        frame.refresh();
    }

    public GameFrame getFrame() {
        return frame;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public Worker getSelectedWorker() {
        return selectedWorker;
    }

    /**
     * The execution logic of movement.
     * Use handleMove instead in case you want to add some leeway to the logic or validateMove to modify any restrictions
     * @param w
     * @param dest
     */
    public void performMove(Worker w, Tile dest) {
        Tile origin = w.getCurrentTile();
        validateMove(w, dest);
        w.moveTo(dest);
        checkWin(origin, dest, w.getPlayer());
    }

    /**
     * The execution logic of building.
     * Use handleBuild instead in case you want to add some leeway to the logic or validateBuild to modify any restrictions
     * @param w
     * @param dest
     */
    public void performBuild(Worker w, Tile dest) {
        validateBuild(w, dest);
        w.build(dest);
    }

    protected void handleSetup(Player current, Tile tile) {
        if (tile.getOccupant() == null) {
            Worker worker1 = current.getWorkers().getFirst();
            Worker worker2 = current.getWorkers().getLast();
            if (worker1.getCurrentTile() == null) {
                worker1.setStartingTile(tile);
            } else if (worker2.getCurrentTile() == null) {
                worker2.setStartingTile(tile);
            }

        } else {
            JOptionPane.showMessageDialog(frame, "Invalid selection!");
        }
        announcePhase();
    }

    /**
     * Worker selection logic
     * @param current
     * @param tile
     */
    protected void handleSelect(Player current, Tile tile) {
        if (tile.getOccupant() != null && tile.getOccupant().getPlayer() == current) {
            selectedWorker = tile.getOccupant();
            phase = Phase.MOVE;
            tile.setSelected(true);
            frame.updateLabels(phase, currentPlayerIndex);
        } else {
            JOptionPane.showMessageDialog(frame, "Invalid selection!");
        }
        announcePhase();
    }

    /**
     * A complete method to execute the move mechanics all together.
     * Just call this one if you don't know what you are doing
     * @param current
     * @param tile
     */
    private void handleMove(Player current, Tile tile) {
        if (selectedWorker != null && tile == selectedWorker.getCurrentTile()) {
            selectedWorker = null;
            phase = Phase.SELECT;
            JOptionPane.showMessageDialog(frame, "Worker deselected. Select another worker.");
            frame.updateLabels(phase, currentPlayerIndex);
            announcePhase();
            return;
        }
        try {
            performMove(selectedWorker, tile);
            phase = Phase.BUILD;
            tile.setSelected(false);
            frame.updateLabels(phase, currentPlayerIndex);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        }
        announcePhase();
    }

    /**
     * Does all the parts of building logic all together.
     * Preferably use this one if given a choice
     * @param current
     * @param tile
     */
    private void handleBuild(Player current, Tile tile) {
        try {
            performBuild(selectedWorker, tile);
            tile.setSelected(false);
            endTurn();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        }
        announcePhase();
    }

    /**
     * Cycle through players and clear all the leftover data
     */
    protected void endTurn() {
        phase = Phase.SELECT;
        selectedWorker = null;
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    /**
     * Self-explanatory. Move restrictions and validation
     * @param w
     * @param target
     */
    protected void validateMove(Worker w, Tile target) {
        Tile origin = w.getCurrentTile();
        if (target.isOccupied() || target.getBuilding().hasDome())
            throw new IllegalArgumentException("Destination blocked");
        int dz = target.getBuilding().getLevel() - origin.getBuilding().getLevel();
        if (dz > 1)
            throw new IllegalArgumentException("Cannot move up more than one level");
        if (dz < -1)
            throw new IllegalArgumentException("Cannot move down more than one level");
        if (Math.abs(target.getX() - origin.getX()) > 1
                || Math.abs(target.getY() - origin.getY()) > 1)
            throw new IllegalArgumentException("Can only move to adjacent tiles");
    }

    /**
     * Self-explanatory. Build restrictions and validation
     * @param w
     * @param target
     */
    protected void validateBuild(Worker w, Tile target) {
        Tile origin = w.getCurrentTile();
        if (target.isOccupied() || target.getBuilding().hasDome())
            throw new IllegalArgumentException("Cannot build here");
        if (Math.abs(target.getX() - origin.getX()) > 1
                || Math.abs(target.getY() - origin.getY()) > 1)
            throw new IllegalArgumentException("Can only build on adjacent tiles");
    }

    /**
     * Checks whether the player has achieved the winning conditions or is yet to do so
     * @param origin
     * @param dest
     * @param player
     */
    private void checkWin(Tile origin, Tile dest, Player player) {
        int oLevel = origin.getBuilding().getLevel();
        int nLevel = dest.getBuilding().getLevel();
        if (oLevel + 1 == nLevel && nLevel == 3) {
            System.out.println(player.getColor() + " wins by reaching level 3!");
            JOptionPane.showMessageDialog(frame, player.getColor() + " wins!");
            System.exit(0);
        }
    }

    /**
     * Prints current player and phase to console
     **/
    protected void announcePhase() {
        Player current = players.get(currentPlayerIndex);
        System.out.println("Turn: " + current.getColor() + " — " + phase.name());
    }

}