package Frame;

import GameEntities.*;
import Frame.Tile;
import Frame.Boards.*;

import javax.swing.*;
import java.util.List;

public class GameController {

    private enum Phase {SETUP, SELECT, MOVE, BUILD, ENDOFTURN, WIN}

    private final Board board;
    private GameFrame frame;
    private final List<Player> players;
    private int currentPlayerIndex = 0;
    private Phase phase = Phase.SELECT;
    private Worker selectedWorker;

    public GameController(Board board, GameFrame frame, List<Player> players) {
        this.board = board;
        this.frame = frame;
        this.players = players;
        announcePhase();
    }

    public void setFrame(GameFrame frame) {
        this.frame = frame;
    }

    public void onTileClicked(Tile tile) {
        Player current = players.get(currentPlayerIndex);
        switch (phase) {
            case SETUP:
                //godcard.handleSetup
                break;
            case SELECT:
                //godcard.handleSelect
                handleSelect(current, tile);
                break;
            case MOVE:
                //godcard.handleMove
                handleMove(current, tile);
                break;
            case BUILD:
                //godCard.handleBuild
                handleBuild(current, tile);
                break;
            case ENDOFTURN:
                //godCard.handleTurnEnd
                break;
            case WIN:
                //godCard.handleWin
                break;
        }
        frame.refresh();
    }

    private void handleSelect(Player current, Tile tile) {
        if (tile.getOccupant() != null && tile.getOccupant().getPlayer() == current) {
            selectedWorker = tile.getOccupant();
            phase = Phase.MOVE;
        } else {
            JOptionPane.showMessageDialog(frame, "Invalid selection!");
        }
        announcePhase();
    }

    private void handleMove(Player current, Tile tile) {
        try {
            Tile origin = selectedWorker.getCurrentTile();
            validateMove(selectedWorker, tile);
            selectedWorker.moveTo(tile);
            current.getGodCard().applyMovePower(selectedWorker, tile);

            int originLevel = origin.getBuilding().getLevel();
            int newLevel    = tile.getBuilding().getLevel();
            if (originLevel + 1 == newLevel && newLevel == 3) {
                System.out.println(current.getColor() + " wins by reaching level 3!");
                JOptionPane.showMessageDialog(frame, current.getColor() + " wins!");
                System.exit(0);
            }

            phase = Phase.BUILD;
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        }
        announcePhase();
    }

    private void handleBuild(Player current, Tile tile) {
        try {
            validateBuild(selectedWorker, tile);
            selectedWorker.build(tile);
            current.getGodCard().applyBuildPower(selectedWorker, tile);
            endTurn();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        }
        announcePhase();
    }

    private void endTurn() {
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

    /** Prints current player and phase to console (currently for testing purposes only) **/
    private void announcePhase() {
        Player current = players.get(currentPlayerIndex);
        System.out.println("Turn: " + current.getColor()
                + " — " + phase.name());
    }

}