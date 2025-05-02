package Frame;

import GameEntities.*;
import Frame.*;
import Frame.Boards.*;

import javax.swing.*;
import java.util.List;

public class GameController implements Controller {

    public enum Phase {SETUP, SELECT, MOVE, BUILD}

    private final Board board;
    private GameFrame frame;
    private final List<Player> players;
    private int currentPlayerIndex = 0;
    private Phase phase;
    private Worker selectedWorker;

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

    public void performMove(Worker w, Tile dest) {
        Tile origin = w.getCurrentTile();
        validateMove(w, dest);
        w.moveTo(dest);
        checkWin(origin, dest, w.getPlayer());
    }

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

    protected void handleSelect(Player current, Tile tile) {
        if (tile.getOccupant() != null && tile.getOccupant().getPlayer() == current) {
            selectedWorker = tile.getOccupant();
            phase = Phase.MOVE;
            tile.setSelected(true);
        } else {
            JOptionPane.showMessageDialog(frame, "Invalid selection!");
        }
        announcePhase();
    }

    private void handleMove(Player current, Tile tile) {
        try {
            performMove(selectedWorker, tile);
            phase = Phase.BUILD;
            tile.setSelected(false);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        }
        announcePhase();
    }

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

    protected void endTurn() {
        phase = Phase.SELECT;
        selectedWorker = null;
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    private void validateMove(Worker w, Tile target) {
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

    private void validateBuild(Worker w, Tile target) {
        Tile origin = w.getCurrentTile();
        if (target.isOccupied() || target.getBuilding().hasDome())
            throw new IllegalArgumentException("Cannot build here");
        if (Math.abs(target.getX() - origin.getX()) > 1
                || Math.abs(target.getY() - origin.getY()) > 1)
            throw new IllegalArgumentException("Can only build on adjacent tiles");
    }

    private void checkWin(Tile origin, Tile dest, Player player) {
        int oLevel = origin.getBuilding().getLevel();
        int nLevel = dest.getBuilding().getLevel();
        if (oLevel + 1 == nLevel && nLevel == 3) {
            System.out.println(player.getColor() + " wins by reaching level 3!");
            JOptionPane.showMessageDialog(frame, player.getColor() + " wins!");
            System.exit(0);
        }
    }

    /** Prints current player and phase to console (currently for testing purposes only) **/
    protected void announcePhase() {
        Player current = players.get(currentPlayerIndex);
        System.out.println("Turn: " + current.getColor() + " — " + phase.name());
    }

}