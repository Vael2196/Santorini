package Frame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class TileButton extends JButton {

    private final Tile tile;
    private final Controller controller;

    public TileButton(Tile tile, Controller controller) {
        this.tile = tile;
        this.controller = controller;
        updateAppearance();
        addActionListener(e -> controller.onTileClicked(tile));
    }

    public void updateAppearance() {
        String text = String.format("<html>Lvl:%d%s%s</html>",
                tile.getBuilding().getLevel(),
                tile.getBuilding().hasDome() ? "<br>Dome" : "",
                tile.isOccupied() ? "<br>" + tile.getOccupant().getPlayer().getColor() : ""
        );
        setText(text);
        //TODO: Set background color based on occupant
    }
}