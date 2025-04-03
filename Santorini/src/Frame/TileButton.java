package Frame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class represents a single tile on the map and the action available to the tile
 * (basically, a connection to the backend Tile;
 * I reckon you'd call that a controller, even though there are some points suggesting against it)
 */
class TileButton extends JButton {
    private int row;
    private int col;
    private Tile tile;

    public TileButton(int row, int col, Tile tile) {
        this.row = row;
        this.col = col;
        this.tile = tile;
        updateDisplay();

        ///honesty, got this idea of making ActionListener from introduction to Java unit. It looks quite neat to me, at least in a logical sense
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // select a builder/move/build (tile highlight basically)
                System.out.println("Frame.Tile clicked at: " + row + ", " + col);
            }
        });
    }

    public void updateDisplay() {
        StringBuilder text = new StringBuilder("<html>");
        text.append("Lvl: ").append(tile.getLevel());
        if (tile.hasDome()) {
            text.append("<br>Dome");
        }
        if (tile.getOccupant() != null) {
            text.append("<br>").append(tile.getOccupant().getPlayer());
        }
        text.append("</html>");
        setText(text.toString());
    }
}
