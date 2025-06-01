package Frame;

import GameEntities.*;
import Frame.Boards.*;

import javax.swing.*;
import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import Frame.GameController;

/**
 * A “Tag” mode controller. Internally wraps a GameController (core),
 * but replaces:
 *   1) Movement rules (can move onto an opponent’s worker of equal level --> immediate win).
 *   2) Building rules (allowed even on occupied tiles, except domes).
 *   3) Skips the “reach level 3 --> win” check entirely.
 * All other phases (SETUP, SELECT, the turn‐rotations, etc.) are delegated to the wrapped GameController.
 */
public class TagController implements Controller {

    private final GameController core;
    private final List<Player> players;
    private Set<Point> holes;
    private boolean hasMoved;

    public TagController(Board board,
                         GameFrame frame,
                         List<Player> players,
                         int workersPerPlayer,
                         Set<Point> holeSet) {
        this.core = new GameController(board, frame, players, workersPerPlayer);
        this.players = players;
        this.holes = new HashSet<>(holeSet);
        this.hasMoved = false;

        this.core.setHoles(holeSet);

        this.core.setPhase(GameController.Phase.SETUP);
    }

    @Override
    public void setFrame(GameFrame frame) {
        core.setFrame(frame);
    }

    @Override
    public void onTileClicked(Tile tile) {
        Player current = players.get(core.getCurrentPlayerIndex());

        switch (core.getPhase()) {
            case SETUP -> {
                core.handleSetup(current, tile);

                boolean allPlaced = true;
                for (Player p : players) {
                    if (p.getWorkers().size() < 1 ||
                            p.getWorkers().get(0).getCurrentTile() == null) {
                        allPlaced = false;
                        break;
                    }
                }
                if (allPlaced) {
                    core.setPhase(GameController.Phase.SELECT);
                } else {
                    core.setCurrentPlayerIndex(
                            (core.getCurrentPlayerIndex() + 1) % players.size()
                    );
                }
                core.getFrame().updateLabels(core.getPhase(), core.getCurrentPlayerIndex());
            }

            case SELECT -> {
                core.handleSelect(current, tile);
            }

            case MOVE -> {
                Worker sel = core.getSelectedWorker();
                if (sel != null && tile == sel.getCurrentTile() && !hasMoved) {
                    core.setPhase(GameController.Phase.SELECT);
                    JOptionPane.showMessageDialog(core.getFrame(),
                            "Worker deselected. Choose another worker.");
                    core.getFrame().updateLabels(core.getPhase(), core.getCurrentPlayerIndex());
                    return;
                }

                try {
                    attemptTagMove(sel, tile);
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(core.getFrame(), ex.getMessage());
                }
            }

            case BUILD -> {
                Worker w = core.getSelectedWorker();
                try {
                    attemptTagBuild(w, tile);
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(core.getFrame(), ex.getMessage());
                }
            }
        }

        core.getFrame().refresh();
    }

    /**
     * Attempt to move “selected worker” onto “tile” under Tag rules.
     * If “tag” (moving onto opponent’s worker of same level) ⇒ immediate win.
     * Otherwise, do a normal move and advance to BUILD phase.
     */
    private void attemptTagMove(Worker w, Tile dest) {
        if (w == null) {
            // No worker selected yet
            throw new IllegalArgumentException("Select a worker first.");
        }

        Tile origin = w.getCurrentTile();
        Point destPoint = new Point(dest.getX(), dest.getY());

        if (holes.contains(destPoint)) {
            throw new IllegalArgumentException("Destination blocked (hole).");
        }

        int originLevel = origin.getBuilding().getLevel();
        int destLevel = dest.getBuilding().getLevel();

        if (dest.isOccupied()) {
            Worker occ = dest.getOccupant();
            if (occ.getPlayer() != w.getPlayer()) {
                if (destLevel == originLevel) {
                    String winner = w.getPlayer().getColor();
                    String loser = occ.getPlayer().getColor();
                    JOptionPane.showMessageDialog(core.getFrame(),
                            winner + " wins by tagging “" + loser + "”!");
                    System.exit(0);
                    return;
                }
                else {
                    throw new IllegalArgumentException("Can only tag opponent on same‐level tile.");
                }
            }
            else {
                throw new IllegalArgumentException("Cannot move onto your own worker.");
            }
        }

        int dx = Math.abs(dest.getX() - origin.getX());
        int dy = Math.abs(dest.getY() - origin.getY());
        if (dx > 1 || dy > 1) {
            throw new IllegalArgumentException("Can only move to adjacent tiles.");
        }
        if (destLevel - originLevel > 1) {
            throw new IllegalArgumentException("Cannot move up more than one level.");
        }
        if (originLevel - destLevel > 1) {
            throw new IllegalArgumentException("Cannot move down more than one level.");
        }

        w.moveTo(dest);
        hasMoved = true;

        core.setPhase(GameController.Phase.BUILD);
        dest.setSelected(false);
        core.getFrame().updateLabels(core.getPhase(), core.getCurrentPlayerIndex());
    }


    private void attemptTagBuild(Worker w, Tile target) {
        if (w == null) {
            throw new IllegalArgumentException("No worker selected to build.");
        }

        Point p = new Point(target.getX(), target.getY());
        if (holes.contains(p)) {
            throw new IllegalArgumentException("Cannot build here (hole).");
        }

        Tile origin = w.getCurrentTile();
        int dx = Math.abs(target.getX() - origin.getX());
        int dy = Math.abs(target.getY() - origin.getY());
        if (dx > 1 || dy > 1) {
            throw new IllegalArgumentException("Can only build on adjacent tiles.");
        }

        if (target.getBuilding().hasDome()) {
            throw new IllegalArgumentException("Cannot build on a dome.");
        }

        w.build(target);

        target.setSelected(false);
        endTurn();
    }

    /**
     * Rotate to next player, reset state:
     */
    private void endTurn() {
        hasMoved = false;
        core.setPhase(GameController.Phase.SELECT);
        core.setCurrentPlayerIndex(
                (core.getCurrentPlayerIndex() + 1) % players.size()
        );
    }


    public int getCurrentPlayerIndex() {
        return core.getCurrentPlayerIndex();
    }

    public GameController.Phase getCorePhase() {
        return core.getPhase();
    }

    public Frame.Boards.Board getCoreBoard() {
        return core.getBoard();
    }


    public List<Player> getPlayers() {
        return players;
    }

    @Override
    public Set<Point> getHoles() {
        return core.getHoles();
    }
}