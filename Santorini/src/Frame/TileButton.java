package Frame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class represents a single tile on the map and the action available to the tile
 * (basically, a connection to the backend Tile;
 * I reckon you'd call that a controller, even though there are some points suggesting against it)
 */
public class TileButton extends JButton {
    private final Tile tile;

    public TileButton(Tile tile) {
        this.tile = tile;
        updateAppearance();
        addActionListener(e -> handleClick());
    }

    private void handleClick() {
        // Hook into controller logic: select/move/build
        System.out.println("Clicked tile: " + tile.getX() + "," + tile.getY());
    }

    public void updateAppearance() {
        String text = String.format("<html>Lvl:%d%s%s</html>",
                tile.getBuilding().getLevel(),
                tile.getBuilding().hasDome() ? "<br>Dome" : "",
                tile.isOccupied() ? "<br>" + tile.getOccupant().getPlayer().getColor() : ""
        );
        setText(text);
        //TODO: Set background color based on occupant, like :contentReference[oaicite:3]{index=3} type shi
    }
}