package Frame;

import GameEntities.*;
import GameEntities.Gods.GodPower;

import javax.swing.*;
import java.util.List;

/**
 * Game logic to run a game of Santorini with God power modifiers
 */
public class GodPowerController implements Controller {
    private final GameController core;
    private final List<Player> players;
    private int idx = 0;
    private GodPower power;
    private boolean moveExecuted;

    /**
     * Constructor method
     * @param core Contains the core game logic, like move and build
     * @param players
     */
    public GodPowerController(GameController core, List<Player> players) {
        this.core = core;
        this.players = players;
        this.power = players.get(0).getGodPower();
        this.moveExecuted = false;
    }

    @Override
    public void setFrame(GameFrame frame) {
        core.setFrame(frame);
    }

    @Override
    public void onTileClicked(Tile tile) {
        Player current = players.get(idx);

        switch (core.getPhase()) {
            case SETUP -> {
                core.handleSetup(current, tile);
                if (current.getWorkers().getLast().getCurrentTile() != null) {
                    if (players.getFirst() == current) {
                        power.resetTurn();
                        core.endTurn();
                        idx = (idx + 1) % players.size();
                        power = players.get(idx).getGodPower();
                        core.setPhase(GameController.Phase.SETUP);
                        core.getFrame().updateLabels(core.getPhase(), idx);
                    } else {
                        endTurn();
                    }
                }
            }

            case SELECT -> core.handleSelect(current, tile);

            case MOVE -> {
                Worker sel = core.getSelectedWorker();
                if (sel != null && tile == sel.getCurrentTile() && !moveExecuted) {
                    core.setPhase(GameController.Phase.SELECT);
                    JOptionPane.showMessageDialog(core.getFrame(),"Selection canceled. Choose a worker.");
                    core.getFrame().updateLabels(core.getPhase(), idx);
                    break;
                }
                try {
                    Worker w = core.getSelectedWorker();
                    Tile origin = w.getCurrentTile();
                    power.beforeMove(w, tile);
                    core.performMove(w, tile);
                    power.afterMove(w, origin, tile);
                    tile.setSelected(false);
                    core.getFrame().updateLabels(core.getPhase(), idx);
                    moveExecuted = true;

                    if (!power.canMoveAgain()) {
                        core.setPhase(GameController.Phase.BUILD);
                        core.getFrame().updateLabels(core.getPhase(), idx);
                    }
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(core.getFrame(), ex.getMessage());
                }
            }

            case BUILD -> {
                try {
                    Worker w = core.getSelectedWorker();
                    power.beforeBuild(w, tile);
                    core.performBuild(w, tile);
                    power.afterBuild(w, tile);
                    tile.setSelected(false);
                    core.getFrame().updateLabels(core.getPhase(), idx);
                    if (!power.canBuildAgain()) {
                        endTurn();
                    }
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(core.getFrame(), ex.getMessage());
                }
            }
        }
        core.getFrame().refresh();
        announcePhase();
    }

    /**
     * Cycle through players and clear all the leftover data
     */
    private void endTurn() {
        power.resetTurn();
        core.endTurn();
        idx = (idx + 1) % players.size();
        power = players.get(idx).getGodPower();
        core.getFrame().updateLabels(core.getPhase(), idx);
        moveExecuted = false;
    }

    /**
     * Prints current player and phase to console
     **/
    private void announcePhase() {
        Player p = players.get(idx);
        var phase = core.getPhase();
        System.out.println("Turn: " + p.getColor() + " — " + phase);

        if (phase == GameController.Phase.MOVE && power.canMoveAgain() ||
                phase == GameController.Phase.BUILD && power.canBuildAgain()) {
            System.out.printf("[%s] %s%n", power.getName(), power.getDescription());
        }
    }

}